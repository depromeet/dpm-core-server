package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.attendance.presentation.dto.request.UpdateAttendanceTimeRequest
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.application.SessionCommandService
import com.server.dpmcore.session.application.config.SessionAttendanceProperties
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.dto.request.SessionCreateRequest
import com.server.dpmcore.session.presentation.mapper.SessionMapper
import com.server.dpmcore.session.presentation.mapper.TimeMapper
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionCommandController(
    private val sessionCommandService: SessionCommandService,
    private val sessionAttendanceProperties: SessionAttendanceProperties,
) : SessionCommandApi {
    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PostMapping
    fun createSession(
        @RequestBody request: SessionCreateRequest,
    ): CustomResponse<Void> {
        sessionCommandService.createSession(
            SessionMapper.toSessionCreateCommand(request, sessionAttendanceProperties.startHour),
        )

        return CustomResponse.ok()
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PatchMapping("/{sessionId}/attendance-time")
    override fun updateAttendanceTime(
        @PathVariable(name = "sessionId") sessionId: SessionId,
        @RequestBody request: UpdateAttendanceTimeRequest,
    ): CustomResponse<Void> {
        sessionCommandService.updateSessionStartTime(
            sessionId = sessionId,
            attendanceStartTime = TimeMapper.localDateTimeToInstant(request.attendanceStartTime),
        )

        return CustomResponse.noContent()
    }
}
