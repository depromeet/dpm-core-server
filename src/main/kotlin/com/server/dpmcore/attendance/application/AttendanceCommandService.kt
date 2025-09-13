package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.application.exception.AttendanceNotFoundException
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.model.AttendanceCheck
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceCreateCommand
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceRecordCommand
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.cohort.application.config.CohortProperties
import com.server.dpmcore.member.member.application.MemberQueryService
import com.server.dpmcore.member.member.application.exception.CohortMembersNotFoundException
import com.server.dpmcore.session.application.SessionQueryService
import com.server.dpmcore.session.application.exception.CheckedAttendanceException
import com.server.dpmcore.session.application.exception.InvalidAttendanceCodeException
import com.server.dpmcore.session.application.exception.TooEarlyAttendanceException
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val sessionQueryService: SessionQueryService,
    private val memberService: MemberQueryService,
    private val cohortProperties: CohortProperties,
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
        validateInputCode(session, command.attendanceCode)

        val status =
            when (val result = session.attend(command.attendedAt)) {
                AttendanceCheck.TooEarly -> throw TooEarlyAttendanceException()
                is AttendanceCheck.Success -> result.status
            }
        attendance.markAttendance(status, command.attendedAt)

        attendancePersistencePort.save(attendance)
        return status
    }

    private fun validateInputCode(
        session: Session,
        inputCode: String,
    ) {
        if (session.isInvalidInputCode(inputCode)) throw InvalidAttendanceCodeException()
    }

    fun updateAttendanceStatus(command: AttendanceStatusUpdateCommand) {
        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId.value, command.memberId.value)
                ?: throw AttendanceNotFoundException()

        attendance.updateStatus(command.attendanceStatus)
        attendancePersistencePort.save(attendance)
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
