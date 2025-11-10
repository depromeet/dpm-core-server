package core.application.session.presentation.controller

import core.application.attendance.presentation.request.UpdateAttendanceTimeRequest
import core.application.cohort.application.properties.CohortProperties
import core.application.common.exception.CustomResponse
import core.application.session.application.service.SessionCommandService
import core.application.session.presentation.mapper.SessionMapper
import core.application.session.presentation.mapper.TimeMapper
import core.application.session.presentation.request.SessionCreateRequest
import core.application.session.presentation.request.SessionUpdateRequest
import core.domain.session.vo.SessionId
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
    private val cohortProperties: CohortProperties,
) : SessionCommandApi {
    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PostMapping
    override fun createSession(
        @RequestBody request: SessionCreateRequest,
    ): CustomResponse<Void> {
        sessionCommandService.createSession(
            SessionMapper.toSessionCreateCommand(request, cohortProperties.value.toLong()),
        )

        return CustomResponse.noContent()
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

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PatchMapping
    override fun updateSession(
        @RequestBody request: SessionUpdateRequest,
    ): CustomResponse<Void> {
        sessionCommandService.updateSession(
            SessionMapper.toSessionUpdateCommand(request),
        )

        return CustomResponse.noContent()
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PatchMapping("/{sessionId}/delete")
    override fun softDeleteSession(
        @PathVariable("sessionId") sessionId: SessionId
    ): CustomResponse<Void> {
        sessionCommandService.softDeleteSession(sessionId)
        return CustomResponse.noContent()
    }
}
