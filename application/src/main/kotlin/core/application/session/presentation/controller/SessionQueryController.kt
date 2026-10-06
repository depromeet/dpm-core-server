package core.application.session.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.application.session.application.service.SessionQueryService
import core.application.session.presentation.mapper.SessionMapper
import core.application.session.presentation.response.AttendanceTimeResponse
import core.application.session.presentation.response.NextSessionResponse
import core.application.session.presentation.response.SessionDetailForDeeperResponse
import core.application.session.presentation.response.SessionDetailResponse
import core.application.session.presentation.response.SessionListResponse
import core.application.session.presentation.response.SessionPolicyUpdateTargetResponse
import core.application.session.presentation.response.SessionWeeksResponse
import core.application.sessionFeedback.application.service.SessionFeedbackFormQueryService
import core.application.sessionFeedback.application.service.SessionFeedbackListQueryService
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDateTime

@RestController
class SessionQueryController(
    private val sessionQueryService: SessionQueryService,
    private val sessionFeedbackFormQueryService: SessionFeedbackFormQueryService,
    private val sessionFeedbackListQueryService: SessionFeedbackListQueryService,
    private val clock: Clock,
) : SessionQueryApi {
    @PreAuthorize("permitAll()")
    @GetMapping("/v1/sessions/next")
    override fun getNextSession(): CustomResponse<NextSessionResponse> {
        val response =
            sessionQueryService
                .getNextSession()
                ?.let { SessionMapper.toNextSessionResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/v1/sessions")
    override fun getAllSessions(): CustomResponse<SessionListResponse> {
        val sessions = sessionQueryService.getAllCurrentCohortSessions()
        val feedbackBySessionId = sessionFeedbackListQueryService.buildFor(sessions, currentMemberIdOrNull())
        val response = SessionMapper.toSessionListResponse(sessions, feedbackBySessionId)

        return CustomResponse.ok(response)
    }

    private fun currentMemberIdOrNull(): MemberId? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication == null ||
            authentication is AnonymousAuthenticationToken ||
            authentication.name == "anonymousUser"
        ) {
            return null
        }
        return runCatching { MemberId(authentication.name.toLong()) }.getOrNull()
    }

    @PreAuthorize("hasAuthority('create:session')")
    @GetMapping("/v1/sessions/{sessionId}")
    override fun getSessionById(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<SessionDetailResponse> {
        val session = sessionQueryService.getSessionById(sessionId)
        val feedbackForm = sessionFeedbackFormQueryService.findBySessionId(sessionId)
        val response = SessionMapper.toSessionDetailResponse(session, feedbackForm, clock)

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasAuthority('read:session')")
    @GetMapping("/v1/sessions/{sessionId}/me")
    override fun getSessionByIdForDeeper(
        @PathVariable(name = "sessionId") sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<SessionDetailForDeeperResponse> {
        val retrieveSession: Session =
            sessionQueryService
                .getSessionById(sessionId)
        val retrieveAttendance = sessionQueryService.getAttendanceBySessionIdAndMemberId(sessionId, memberId)

        return CustomResponse.ok(
            SessionMapper.toSessionDetailForDeeperResponse(
                session = retrieveSession,
                attendance = retrieveAttendance,
            ),
        )
    }

    @PreAuthorize("hasAuthority('update:session')")
    @GetMapping("/v1/sessions/{sessionId}/attendance-time")
    override fun getAttendanceTime(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<AttendanceTimeResponse> {
        val response =
            sessionQueryService
                .getAttendancePolicy(sessionId)
                .let { SessionMapper.toAttendanceTimeResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasAuthority('read:session')")
    @GetMapping("/v1/sessions/weeks")
    override fun getSessionWeeks(): CustomResponse<SessionWeeksResponse> {
        val response =
            sessionQueryService
                .getSessionWeeks()
                .let { SessionMapper.toSessionWeeksResponse(it) }

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasAuthority('update:session')")
    @GetMapping("/v3/sessions/{sessionId}/update-policy")
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
