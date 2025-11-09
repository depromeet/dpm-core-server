package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.cohort.application.properties.CohortProperties
import core.application.member.application.exception.CohortMembersNotFoundException
import core.application.member.application.service.MemberQueryService
import core.application.session.application.exception.CheckedAttendanceException
import core.application.session.application.exception.TooEarlyAttendanceException
import core.application.session.application.service.SessionQueryService
import core.application.session.application.validator.SessionValidator
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceCreateCommand
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.vo.AttendanceResult
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val sessionQueryService: SessionQueryService,
    private val memberService: MemberQueryService,
    private val cohortProperties: CohortProperties,
    private val sessionValidator: SessionValidator,
) {
    fun attendSession(command: AttendanceRecordCommand): AttendanceStatus {
        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId.value, command.memberId.value)
                ?.also {
                    if (it.isAttended()) {
                        throw CheckedAttendanceException()
                    }
                } ?: throw AttendanceNotFoundException()

        val session = sessionQueryService.getSessionById(command.sessionId)
        sessionValidator.validateInputCode(session, command.attendanceCode)

        val status =
            when (val result = session.attend(command.attendedAt)) {
                AttendanceResult.TooEarly -> throw TooEarlyAttendanceException()
                is AttendanceResult.Success -> result.status
            }
        attendance.markAttendance(status, command.attendedAt)

        attendancePersistencePort.save(attendance)
        return status
    }

    fun updateAttendanceStatus(command: AttendanceStatusUpdateCommand) {
        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId.value, command.memberId.value)
                ?: throw AttendanceNotFoundException()

        attendance.updateStatus(command.attendanceStatus)
        attendancePersistencePort.save(attendance)
    }

    fun updateAttendanceStatusBulk(
        sessionId: SessionId,
        attendanceStatus: AttendanceStatus,
        memberIds: List<MemberId>,
    ) {
        val attendances = memberIds.map { memberId ->
            attendancePersistencePort.findAttendanceBy(sessionId.value, memberId.value)
                ?.apply { updateStatus(attendanceStatus) }
                ?: throw AttendanceNotFoundException()
        }

        attendancePersistencePort.updateInBatch(attendances)
    }

    fun createAttendances(sessionId: SessionId) {
        val memberIds = memberService.getMembersByCohort(cohortProperties.value)
        if (memberIds.isEmpty()) {
            throw CohortMembersNotFoundException()
        }

        val attendances =
            memberIds
                .map { memberId ->
                    Attendance.create(
                        AttendanceCreateCommand(sessionId, memberId),
                    )
                }

        attendancePersistencePort.saveInBatch(attendances)
    }
}
