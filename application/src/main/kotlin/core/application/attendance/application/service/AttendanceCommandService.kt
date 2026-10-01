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
 * 출석 기록 쓰기.
 *
 * 동시성 규칙
 * - 모든 쓰기는 세션 행 잠금을 먼저 잡고 출석 행을 갱신한다(잠금 순서: session -> attendance).
 * - 인증만 세션 공유 잠금(FOR SHARE)을 써서 서로 동시에 진행한다. 인증은 PK 로 한 행만 잠근다.
 * - 운영진 변경(단건/일괄/사유 승인), 세션 시각 변경, 정책 재계산, 삭제는 세션 쓰기 잠금(FOR UPDATE)으로 직렬화한다.
 * - 출석 행은 읽은 엔티티 전체를 저장하지 않고 DB 에서 조건을 확인하는 UPDATE 로만 바꾼다.
 * - 운영진 변경만 updatedAt 을 기록한다. 정책 재계산은 updatedAt 을 기록하지 않는다.
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
     * 출석 인증. [AttendanceRecordCommand.attendedAt] 은 컨트롤러가 요청을 받은 시각이다.
     *
     * - 정확히 지각 시작 시각이면 LATE, 정확히 마감 시각부터는 마감 오류이며 아무것도 저장하지 않는다.
     * - 코드가 틀리거나 너무 이르면 아무것도 저장하지 않는다.
     * - 운영진이 정한 기록(updatedAt 존재)은 attendedAt 이 없어도 덮어쓰지 않는다.
     * - 실제로 저장된 경우에만 판정 결과를 반환한다. 동시에 다른 요청이 먼저 저장했다면 이미 출석 오류다.
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

    /** 운영진 단건 변경(결석 사유 승인 포함). attendedAt 은 보존하고 상태와 updatedAt 만 바꾼다. */
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

    /** 운영진 일괄 변경. 대상 중 하나라도 출석 기록이 없으면 아무것도 바꾸지 않는다. attendedAt 은 보존한다. */
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

    /**
     * 세션 출석 시각 변경을 출석 기록에 반영한다. 호출자는 이미 [session] 행 쓰기 잠금을 잡은 트랜잭션이어야 한다.
     *
     * 판정 규칙은 [Attendance.recalculateStatusByPolicy] 하나만 사용하므로 변경 대상 미리보기와 실제 반영이 같다.
     * 운영진 변경 기록은 보호하고, 재계산은 updatedAt 을 기록하지 않는다.
     *
     * @return 상태가 바뀐 기록 수
     */
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
                attendancePersistencePort.updateStatusByPolicy(attendanceId, attendance.status, newStatus)
            }
    }

    /**
     * 세션의 최신 출석 시각으로 출석 기록을 다시 맞춘다. 이벤트 값이 아닌, 잠금을 잡고 읽은 현재 세션 값을 사용한다.
     */
    fun reconcileAttendancesWithLatestPolicy(sessionId: SessionId): Int {
        val session = sessionPersistencePort.findSessionByIdForUpdate(sessionId.value) ?: return 0
        return applySessionPolicyChange(session, clock.instant())
    }

    /**
     * 새 세션의 초기 출석 기록을 만든다. 세션 생성 트랜잭션 안(BEFORE_COMMIT)에서 호출되어 세션과 함께 커밋/롤백된다.
     *
     * 멤버가 아직 없는 기수는 기록 없이 끝낸다. (이전에는 커밋 이후 예외를 던졌지만 세션은 이미 저장된 상태였다.
     * 같은 트랜잭션으로 옮기면서 예외를 그대로 두면 멤버 없는 기수의 세션 생성 자체가 실패하므로 동작을 유지하려고 건너뛴다.)
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

    /** 세션의 출석 기록을 소프트 삭제한다. 이미 삭제된 기록은 건드리지 않으므로 여러 번 호출해도 안전하다. */
    fun deleteAttendancesBySessionId(
        sessionId: SessionId,
        deletedAt: Instant,
    ) {
        attendancePersistencePort.softDeleteAllBySessionId(sessionId.value, deletedAt)
    }

    /**
     * 새로 활성화된 멤버의 출석 기록을 만든다.
     *
     * - 처리 시각 기준으로 인증 마감이 아직 지나지 않은(마감 > 현재) 세션에만 만든다. 정확히 마감 시각인 세션도 제외한다.
     *   가입 전에 이미 끝난 세션의 미인증 기록을 만들어 가입 전 결석이 부과될 여지를 남기지 않기 위해서다.
     * - 이미 있는 기록은 그대로 둔다.
     * - 세션 공유 잠금을 세션 ID 오름차순으로 잡고 다시 읽어, 동시에 진행되는 세션 삭제/시각 변경과 직렬화한다.
     */
    fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        val now = clock.instant()
        val sessionIds =
            sessionPersistencePort
                .findAllCohortSessions(cohortId.value)
                .mapNotNull { it.id?.value }
                .distinct()
                .sorted()

        val attendances =
            sessionIds.mapNotNull { sessionId ->
                val session = sessionPersistencePort.findSessionByIdForShare(sessionId) ?: return@mapNotNull null
                if (session.isAttendanceClosedAt(now)) return@mapNotNull null
                val existing = attendancePersistencePort.findAttendanceBy(sessionId, memberId.value)
                if (existing != null) return@mapNotNull null

                Attendance.create(AttendanceCreateCommand(sessionId = SessionId(sessionId), memberId = memberId))
            }

        attendancePersistencePort.saveInBatch(attendances)
    }
}
