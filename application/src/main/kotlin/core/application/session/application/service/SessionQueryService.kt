package core.application.session.application.service

import core.application.attendance.application.service.AttendanceQueryService
import core.application.member.application.exception.MemberNotFoundException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.application.session.presentation.response.SessionPolicyUpdateTargetResponse
import core.domain.attendance.aggregate.Attendance
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.extension.hasChangedComparedTo
import core.domain.session.port.inbound.command.SessionAttendancePolicyCommand
import core.domain.session.port.inbound.query.SessionWeekQueryModel
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class SessionQueryService(
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendanceQueryService: AttendanceQueryService,
    private val memberQueryUseCase: MemberQueryUseCase
) {
    fun getNextSession(): Session? {
        val koreaZone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.ofInstant(Instant.now(), koreaZone)
        val startOfToday = today.atStartOfDay(koreaZone).toInstant()

        return sessionPersistencePort.findNextSessionBy(startOfToday)
    }

    fun getAllSessions(): List<Session> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort.findAllSessions(cohortId.value)
    }

    fun getSessionById(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionById(sessionId.value)
            ?: throw SessionNotFoundException()

    fun getAttendanceTime(sessionId: SessionId): Instant =
        sessionPersistencePort
            .findSessionById(sessionId.value)
            ?.attendancePolicy
            ?.attendanceStart
            ?: throw SessionNotFoundException()

    fun getSessionWeeks(): List<SessionWeekQueryModel> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort
            .findAllSessions(cohortId.value)
            .map { SessionWeekQueryModel(sessionId = it.id!!, week = it.week, date = it.date) }
    }

    fun queryTargetAttendancesByPolicyChange(command: SessionAttendancePolicyCommand): SessionPolicyUpdateTargetResponse {
        val currentPolicy = getCurrentPolicy(command.sessionId.value)

        if (!isPolicyChanged(currentPolicy, command)) {
            return SessionPolicyUpdateTargetResponse(emptyList(), emptyList())
        }

        val attendances = attendanceQueryService.getAttendancesBySessionId(command.sessionId)
        return classifyAttendancesByPolicyChange(attendances, command)
    }

    private fun getCurrentPolicy(sessionId: Long): AttendancePolicy =
        sessionPersistencePort.findSessionById(sessionId)
            ?.attendancePolicy
            ?: throw SessionNotFoundException()

    private fun isPolicyChanged(
        currentPolicy: AttendancePolicy,
        command: SessionAttendancePolicyCommand,
    ): Boolean {
        return currentPolicy.hasChangedComparedTo(
            command.attendanceStart,
            command.lateStart,
            command.absentStart,
        )
    }

    private fun classifyAttendancesByPolicyChange(
        attendances: List<Attendance>,
        command: SessionAttendancePolicyCommand,
    ): SessionPolicyUpdateTargetResponse {
        val memberIds = attendances.map { it.memberId }.distinct()
        val membersById = memberQueryUseCase.getMembersByIds(memberIds).associateBy { it.id }

        val targeted = mutableListOf<SessionPolicyUpdateTargetResponse.TargetedResponse>()
        val untargeted = mutableListOf<SessionPolicyUpdateTargetResponse.UntargetedResponse>()

        attendances.forEach { attendance ->
            if (attendance.isStatusChangedByPolicy(command.lateStart, command.absentStart)) {
                targeted += createTargetedResponse(attendance, command, membersById)
            } else if (attendance.isAlreadyUpdated()) {
                untargeted += createUntargetedResponse(attendance, membersById)
            }
        }

        return SessionPolicyUpdateTargetResponse(targeted, untargeted)
    }

    private fun createTargetedResponse(
        attendance: Attendance,
        command: SessionAttendancePolicyCommand,
        membersById: Map<MemberId?, Member>,
    ): SessionPolicyUpdateTargetResponse.TargetedResponse {
        return SessionPolicyUpdateTargetResponse.TargetedResponse(
            name = membersById[attendance.memberId]?.name ?: throw MemberNotFoundException(),
            currentStatus = attendance.status.name,
            targetStatus = attendance.simulateStatusChange(command.lateStart, command.absentStart).name,
            attendedAt = instantToLocalDateTime(attendance.attendedAt),
        )
    }

    private fun createUntargetedResponse(
        attendance: Attendance,
        membersById: Map<MemberId?, Member>,
    ): SessionPolicyUpdateTargetResponse.UntargetedResponse {
        return SessionPolicyUpdateTargetResponse.UntargetedResponse(
            name = membersById[attendance.memberId]?.name ?: throw MemberNotFoundException(),
            status = attendance.status.name,
            updatedAt = instantToLocalDateTime(attendance.updatedAt),
        )
    }
}
