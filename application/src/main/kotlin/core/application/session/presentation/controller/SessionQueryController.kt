package core.application.session.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.session.application.service.SessionQueryService
import core.application.session.presentation.mapper.SessionMapper
import core.application.session.presentation.response.AttendanceTimeResponse
import core.application.session.presentation.response.NextSessionResponse
import core.application.session.presentation.response.SessionDetailResponse
import core.application.session.presentation.response.SessionListResponse
import core.application.session.presentation.response.SessionPolicyUpdateTargetResponse
import core.application.session.presentation.response.SessionWeeksResponse
import core.domain.session.vo.SessionId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/sessions")
class SessionQueryController(
    private val sessionQueryService: SessionQueryService,
) : SessionQueryApi {
    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/next")
    override fun getNextSession(): CustomResponse<NextSessionResponse> {
        val response =
            sessionQueryService
                .getNextSession()
                ?.let { SessionMapper.toNextSessionResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping
    override fun getAllSessions(): CustomResponse<SessionListResponse> {
        val response =
            sessionQueryService
                .getAllSessions()
                .let { SessionMapper.toSessionListResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/{sessionId}")
    override fun getSessionById(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<SessionDetailResponse> {
        val response =
            sessionQueryService
                .getSessionById(sessionId)
                .let { SessionMapper.toSessionDetailResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{sessionId}/attendance-time")
    override fun getAttendanceTime(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<AttendanceTimeResponse> {
        val response =
            sessionQueryService
                .getAttendanceTime(sessionId)
                .let { SessionMapper.toAttendanceTimeResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/weeks")
    override fun getSessionWeeks(): CustomResponse<SessionWeeksResponse> {
        val response =
            sessionQueryService
                .getSessionWeeks()
                .let { SessionMapper.toSessionWeeksResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/{sessionId}/update-policy")
    override fun queryTargetAttendancesByPolicyChange(
        @PathVariable("sessionId") sessionId: SessionId,
        @RequestParam(value = "attendanceStart", required = true) attendanceStart: LocalDateTime,
        @RequestParam(value = "lateStart", required = true) lateStart: LocalDateTime,
        @RequestParam(value = "absentStart", required = true) absentStart: LocalDateTime,
    ): CustomResponse<SessionPolicyUpdateTargetResponse> {
        val response =
            sessionQueryService.queryTargetAttendancesByPolicyChange(
                SessionMapper.toSessionAttendancePolicyChangedCommand(
                    sessionId,
                    attendanceStart,
                    lateStart,
                    absentStart,
                ),
            )

        return CustomResponse.ok(response)
    }
}
