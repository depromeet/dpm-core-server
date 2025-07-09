package com.server.dpmcore.attendance.presentation

import com.server.dpmcore.attendance.application.AttendanceCommandService
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceCreateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper.toAttendanceResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class AttendanceController(
    private val attendanceCommandService: AttendanceCommandService,
) {
    @PostMapping("/v1/sessions/{sessionId}/attendances")
    fun createAttendance(
        @PathVariable sessionId: SessionId,
        @RequestBody request: AttendanceCreateRequest,
    ): CustomResponse<AttendanceResponse> {
        val attendedAt = Instant.now()
        val attendanceStatus = attendanceCommandService.attendSession(sessionId, attendedAt, request)

        return CustomResponse.ok(toAttendanceResponse(attendanceStatus, attendedAt))
    }
}
