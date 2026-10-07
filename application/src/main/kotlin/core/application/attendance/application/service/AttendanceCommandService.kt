package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.session.application.exception.AttendanceAlreadyDecidedException
import core.application.session.application.exception.AttendanceClosedException
import core.application.session.application.exception.CheckedAttendanceException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.application.exception.TooEarlyAttendanceException
import core.application.session.application.validator.SessionValidator
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceCreateCommand
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.vo.AttendanceResult
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 잠금 순서는 세션 행 -> 출석 행이다. 인증과 자동 결석은 세션 공유 잠금으로 서로 동시에 진행하고
 * (둘 다 출석 행을 단일 UPDATE 로만 잠가 교착이 없다), 운영진 변경/시각 변경/삭제는 세션 쓰기 잠금으로 직렬화한다.
 * 출석 행은 조건부 UPDATE 로만 바꾼다. 자동 결석만 autoAbsentAt 표지를 남기고 인증/운영진 변경/재개는 표지를 지운다.
 */
@Service
@Transactional
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val sessionPersistencePort: SessionPersistencePort,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val sessionValidator: SessionValidator,
    private val clock: Clock,
) {
    /**
     * 마감 전에 접수된 요청은 자동 결석이 먼저 저장됐어도 정상 판정으로 저장한다.
     * 조건부 UPDATE 가 실패하면(다른 요청이 먼저 저장) 이미 출석 오류다.
     */
    fun attendSession(command: AttendanceRecordCommand): AttendanceStatus {
        val session =
            sessionPersistencePort.findSessionByIdForShare(command.sessionId.value)
                ?: throw SessionNotFoundException()

        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId.value, command.memberId.value)
                ?: throw AttendanceNotFoundException()

        if (attendance.isAlreadyUpdated()) throw AttendanceAlreadyDecidedException()
        if (!attendance.canRecordAttendance()) throw CheckedAttendanceException()

        sessionValidator.validateInputCode(session, command.attendanceCode)

        val status =
            when (val result = session.attend(command.attendedAt)) {
                AttendanceResult.TooEarly -> throw TooEarlyAttendanceException()
                AttendanceResult.Closed -> throw AttendanceClosedException()
                is AttendanceResult.Success -> result.status
            }

        val attendanceId = attendance.id?.value ?: throw AttendanceNotFoundException()
        val recorded = attendancePersistencePort.recordAttendanceIfAllowed(attendanceId, status, command.attendedAt)
        if (!recorded) throw CheckedAttendanceException()

        return status
    }

    /** 운영진 변경. 출석 인증 시각(attendedAt)은 지운다. */
    fun updateAttendanceStatus(command: AttendanceStatusUpdateCommand) {
        sessionPersistencePort.findSessionByIdForUpdate(command.sessionId.value)
            ?: throw SessionNotFoundException()

        val updated =
            attendancePersistencePort.updateStatusByAdmin(
                sessionId = command.sessionId.value,
                memberIds = listOf(command.memberId.value),
                status = command.attendanceStatus,
                updatedAt = clock.instant(),
            )
        if (updated == 0) throw AttendanceNotFoundException()
    }

    /** 대상 중 하나라도 출석 기록이 없으면 아무것도 바꾸지 않는다. */
    fun updateAttendanceStatusBulk(
        sessionId: SessionId,
        attendanceStatus: AttendanceStatus,
        memberIds: List<MemberId>,
    ) {
        sessionPersistencePort.findSessionByIdForUpdate(sessionId.value)
            ?: throw SessionNotFoundException()

        val targetMemberIds = memberIds.map { it.value }.distinct().sorted()
        if (targetMemberIds.isEmpty()) return

        val existingCount = attendancePersistencePort.countActiveAttendances(sessionId.value, targetMemberIds)
        if (existingCount != targetMemberIds.size) throw AttendanceNotFoundException()

        attendancePersistencePort.updateStatusByAdmin(
            sessionId = sessionId.value,
            memberIds = targetMemberIds,
            status = attendanceStatus,
            updatedAt = clock.instant(),
        )
    }

    /** 호출자가 세션 쓰기 잠금을 잡은 트랜잭션이어야 한다. 운영진 변경 기록은 보호하고 updatedAt 을 남기지 않는다. */
    @Transactional(propagation = Propagation.MANDATORY)
    fun applySessionPolicyChange(
        session: Session,
        now: Instant,
    ): Int {
        val sessionId = session.id ?: throw SessionNotFoundException()
        val policy = session.attendancePolicy

        return attendancePersistencePort
            .findAllBySessionId(sessionId.value)
            .count { attendance ->
                val newStatus =
                    attendance.recalculateStatusByPolicy(policy.lateStart, policy.absentStart, now)
                        ?: return@count false
                val attendanceId = attendance.id?.value ?: return@count false
                if (newStatus == AttendanceStatus.PENDING) {
                    // 자동 결석(표지 있음) 재개. 표지 없는 기존 결석은 조건에서 걸러진다.
                    attendancePersistencePort.reopenAutoAbsence(attendanceId)
                } else {
                    attendancePersistencePort.updateStatusByPolicy(attendanceId, attendance.status, newStatus)
                }
            }
    }

    /** 이벤트 값이 아니라 잠금 후 읽은 최신 세션 시각으로 맞춘다. */
    fun reconcileAttendancesWithLatestPolicy(sessionId: SessionId): Int {
        val session = sessionPersistencePort.findSessionByIdForUpdate(sessionId.value) ?: return 0
        return applySessionPolicyChange(session, clock.instant())
    }

    /**
     * 공유 잠금 후 최신 마감을 다시 확인해, 그 사이 마감이 연장됐거나 세션이 삭제됐으면 처리하지 않는다.
     * 기수는 호출자가 고르고(활성 기수), 현재 PENDING 인 기록만 바꾼다. 운영진이 PENDING 으로 되돌린 기록도 결석이 된다.
     */
    fun closeExpiredAttendances(
        sessionId: SessionId,
        now: Instant,
    ): Int {
        val session = sessionPersistencePort.findSessionByIdForShare(sessionId.value) ?: return 0

        if (!session.isAttendanceClosedAt(now)) return 0

        return attendancePersistencePort.markAutoAbsence(sessionId.value, now)
    }

    /**
     * 세션 생성 트랜잭션 안(BEFORE_COMMIT)에서 호출돼 세션과 함께 커밋/롤백된다.
     * 멤버가 없는 기수에서도 세션 생성이 실패하지 않도록 기록 없이 끝낸다.
     */
    fun createAttendances(
        sessionId: SessionId,
        cohortId: CohortId,
    ) {
        val memberIds: List<MemberId> = memberQueryUseCase.getMemberIdsByCohortId(cohortId)
        if (memberIds.isEmpty()) return

        val attendances =
            memberIds
                .map { memberId ->
                    Attendance.create(
                        AttendanceCreateCommand(sessionId, memberId),
                    )
                }

        attendancePersistencePort.saveInBatch(attendances)
    }

    fun deleteAttendancesBySessionId(
        sessionId: SessionId,
        deletedAt: Instant,
    ) {
        attendancePersistencePort.softDeleteAllBySessionId(sessionId.value, deletedAt)
    }

    /** 기수 세션 중 출석 기록이 없는 세션에만 만든다. 기존 기록은 그대로 둔다. */
    fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        val attendances =
            sessionPersistencePort
                .findAllCohortSessions(cohortId.value)
                .mapNotNull { it.id }
                .filter { attendancePersistencePort.findAttendanceBy(it.value, memberId.value) == null }
                .map { sessionId -> Attendance.create(AttendanceCreateCommand(sessionId, memberId)) }

        attendancePersistencePort.saveInBatch(attendances)
    }
}
