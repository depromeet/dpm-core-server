package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.domain.exception.AttendanceNotFoundException
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceCreateCommand
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceRecordCommand
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.cohort.application.config.CohortProperties
import com.server.dpmcore.member.member.application.MemberService
import com.server.dpmcore.session.application.SessionQueryService
import com.server.dpmcore.session.domain.exception.CheckedAttendanceException
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val sessionQueryService: SessionQueryService,
    private val memberService: MemberService,
    private val cohortProperties: CohortProperties,
) {
    fun attendSession(command: AttendanceRecordCommand): AttendanceStatus {
        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId, command.memberId)
                ?.also {
                    if (it.isAttended()) {
                        throw CheckedAttendanceException()
                    }
                } ?: throw AttendanceNotFoundException()

        val status =
            sessionQueryService
                .getSessionById(command.sessionId)
                .attend(command.attendedAt, command.attendanceCode)

        attendance.markAttendance(status, command.attendedAt)
        attendancePersistencePort.save(attendance)

        return status
    }

    fun updateAttendanceStatus(command: AttendanceStatusUpdateCommand) {
        val attendance =
            attendancePersistencePort
                .findAttendanceBy(command.sessionId, command.memberId)
                ?: throw AttendanceNotFoundException()

        attendance.updateStatus(command.attendanceStatus)
        attendancePersistencePort.save(attendance)
    }

    fun createAttendances(sessionId: SessionId) {
        val memberIds = memberService.getMembersByCohort(cohortProperties.value)

        val attendances =
            memberIds
                .map { memberId ->
                    Attendance.create(
                        AttendanceCreateCommand(sessionId, memberId),
                    )
                }.toList()

        attendancePersistencePort.saveInBatch(attendances)
    }
}
