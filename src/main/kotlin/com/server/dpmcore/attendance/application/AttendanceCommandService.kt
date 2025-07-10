package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceCreateRequest
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.application.SessionQueryService
import com.server.dpmcore.session.domain.exception.CheckedAttendanceException
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val sessionQueryService: SessionQueryService,
) {
    fun attendSession(
        sessionId: SessionId,
        attendedAt: Instant,
        request: AttendanceCreateRequest,
    ): AttendanceStatus {
        val memberId = MemberId(request.memberId)

        val attendance =
            attendancePersistencePort
                .findAttendanceBy(sessionId, memberId)
                .also {
                    if (it.isAttended()) {
                        throw CheckedAttendanceException()
                    }
                }

        val status =
            sessionQueryService
                .getSessionById(sessionId)
                .attend(attendedAt, request.attendanceCode)

        attendance.markAttendance(status, attendedAt)
        attendancePersistencePort.save(attendance)

        return status
    }
}
