package core.application.session.application.service

import core.application.attendance.application.service.AttendanceQueryService
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.member.application.exception.MemberNotFoundException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.presentation.mapper.SessionMapper
import core.application.session.presentation.response.NextSessionHomeResponse
import core.application.session.presentation.response.SessionPolicyUpdateTargetResponse
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.enums.NextSessionHomeStatus
import core.domain.session.port.inbound.command.SessionAttendancePolicyCommand
import core.domain.session.port.inbound.query.SessionSelectorQueryModel
import core.domain.session.port.inbound.query.SessionWeekQueryModel
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class SessionQueryService(
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendanceQueryService: AttendanceQueryService,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val clock: Clock,
) {
    fun getNextSessionHome(memberId: MemberId): NextSessionHomeResponse {
        val member = memberQueryUseCase.getMemberById(memberId)
        val memberCohortId = member.latestCohortId()
        val memberCohortValue = member.latestCohortValue()
        val activeCohortId = cohortQueryUseCase.findActiveCohortId()

        if (memberCohortId == null || activeCohortId == null || memberCohortId != activeCohortId) {
            return NextSessionHomeResponse(
                status = NextSessionHomeStatus.COHORT_ENDED,
                cohortValue = memberCohortValue,
                session = null,
            )
        }

        val koreaZone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.ofInstant(clock.instant(), koreaZone)
        val startOfToday = today.atStartOfDay(koreaZone).toInstant()
        val nextSession = sessionPersistencePort.findNextSessionBy(memberCohortId.value, startOfToday)

        return if (nextSession != null) {
            NextSessionHomeResponse(
                status = NextSessionHomeStatus.AVAILABLE,
                cohortValue = memberCohortValue,
                session = SessionMapper.toNextSessionResponse(nextSession),
            )
        } else {
            NextSessionHomeResponse(
                status = NextSessionHomeStatus.NOT_REGISTERED,
                cohortValue = memberCohortValue,
                session = null,
            )
        }
    }

    fun getAllCurrentCohortSessions(): List<Session> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort.findAllCohortSessions(cohortId.value)
    }

    fun getAllCohortSessions(cohortId: CohortId): List<Session> =
        sessionPersistencePort.findAllCohortSessions(cohortId.value)

    fun getSessionById(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionById(sessionId.value)
            ?: throw SessionNotFoundException()

    fun getAttendanceBySessionIdAndMemberId(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance =
        attendanceQueryService.getAttendancesBy(
            sessionId = sessionId,
            memberId = memberId,
        )

    fun getAttendancePolicy(sessionId: SessionId): AttendancePolicy =
        sessionPersistencePort
            .findSessionById(sessionId.value)
            ?.attendancePolicy
            ?: throw SessionNotFoundException()

    fun getSessionWeeks(): List<SessionWeekQueryModel> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort
            .findAllCohortSessions(cohortId.value)
            .map { SessionWeekQueryModel(sessionId = it.id!!, week = it.week, date = it.date) }
    }

    /**
     * 현재 활성 기수 세션을 일시, ID 오름차순으로 준다. 주차는 표시용이라 정렬에 쓰지 않는다.
     * 출석 인증 상태는 한 번 읽은 현재 시각으로 모든 세션을 판단한다.
     */
    fun getSessionSelector(): List<SessionSelectorQueryModel> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()
        val now = clock.instant()

        return sessionPersistencePort
            .findAllCohortSessions(cohortId.value)
            .sortedWith(compareBy<Session>({ it.date }, { it.id!!.value }))
            .map {
                SessionSelectorQueryModel(
                    sessionId = it.id!!,
                    week = it.week,
                    date = it.date,
                    eventName = it.eventName,
                    place = it.place,
                    isOnline = it.isOnline,
                    attendanceStatus = it.attendanceStatusAt(now),
                )
            }
    }

    fun queryTargetAttendancesByPolicyChange(
        command: SessionAttendancePolicyCommand,
    ): SessionPolicyUpdateTargetResponse {
        val currentPolicy = getCurrentPolicy(command.sessionId.value)

        if (!isPolicyChanged(currentPolicy, command)) {
            return SessionPolicyUpdateTargetResponse(emptyList(), emptyList())
        }

        val attendances = attendanceQueryService.getAttendancesBySessionId(command.sessionId)
        return classifyAttendancesByPolicyChange(attendances, command)
    }

    private fun getCurrentPolicy(sessionId: Long): AttendancePolicy =
        sessionPersistencePort
            .findSessionById(sessionId)
            ?.attendancePolicy
            ?: throw SessionNotFoundException()

    private fun isPolicyChanged(
        currentPolicy: AttendancePolicy,
        command: SessionAttendancePolicyCommand,
    ): Boolean =
        currentPolicy.attendanceStart != command.attendanceStart ||
            currentPolicy.lateStart != command.lateStart ||
            currentPolicy.absentStart != command.absentStart

    private fun classifyAttendancesByPolicyChange(
        attendances: List<Attendance>,
        command: SessionAttendancePolicyCommand,
    ): SessionPolicyUpdateTargetResponse {
        val memberIds = attendances.map { it.memberId }.distinct()

        val members = memberQueryUseCase.getMembersByIds(memberIds)
        val activeMemberIds = members.map { it.id }.toSet()
        val membersById = members.associateBy { it.id }

        val validAttendances = attendances.filter { it.memberId in activeMemberIds }

        val targeted = mutableListOf<SessionPolicyUpdateTargetResponse.TargetedResponse>()
        val untargeted = mutableListOf<SessionPolicyUpdateTargetResponse.UntargetedResponse>()

        // 실제 반영과 같은 규칙(recalculateStatusByPolicy)으로 미리 계산한다.
        val now = clock.instant()
        validAttendances.forEach { attendance ->
            val targetStatus = attendance.recalculateStatusByPolicy(command.lateStart, command.absentStart, now)
            if (targetStatus != null) {
                targeted += createTargetedResponse(attendance, targetStatus, membersById)
            } else if (attendance.isAlreadyUpdated()) {
                untargeted += createUntargetedResponse(attendance, membersById)
            }
        }

        return SessionPolicyUpdateTargetResponse(targeted, untargeted)
    }

    private fun createTargetedResponse(
        attendance: Attendance,
        targetStatus: AttendanceStatus,
        membersById: Map<MemberId?, Member>,
    ): SessionPolicyUpdateTargetResponse.TargetedResponse =
        SessionPolicyUpdateTargetResponse.TargetedResponse(
            name = membersById[attendance.memberId]?.name ?: throw MemberNotFoundException(),
            currentStatus = attendance.status.name,
            targetStatus = targetStatus.name,
            attendedAt = instantToLocalDateTime(attendance.attendedAt),
        )

    private fun createUntargetedResponse(
        attendance: Attendance,
        membersById: Map<MemberId?, Member>,
    ): SessionPolicyUpdateTargetResponse.UntargetedResponse =
        SessionPolicyUpdateTargetResponse.UntargetedResponse(
            name = membersById[attendance.memberId]?.name ?: throw MemberNotFoundException(),
            status = attendance.status.name,
            updatedAt = instantToLocalDateTime(attendance.updatedAt),
        )
}
