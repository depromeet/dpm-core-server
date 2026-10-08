package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.application.sessionFeedback.application.service.SessionFeedbackMyQueryService
import core.application.sessionFeedback.application.service.SessionFeedbackPendingQueryService
import core.application.sessionFeedback.presentation.response.SessionFeedbackMyResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackPendingResponse
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionFeedbackQueryController(
    private val sessionFeedbackMyQueryService: SessionFeedbackMyQueryService,
    private val sessionFeedbackPendingQueryService: SessionFeedbackPendingQueryService,
) : SessionFeedbackQueryApi {
    @PreAuthorize("hasAuthority('read:session')")
    @GetMapping("/v1/sessions/{sessionId}/feedbacks/me")
    override fun getMyFeedback(
        @PathVariable(name = "sessionId") sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<SessionFeedbackMyResponse> =
        CustomResponse.ok(sessionFeedbackMyQueryService.getMyFeedback(sessionId, memberId))

    @PreAuthorize("hasAuthority('read:session')")
    @GetMapping("/v1/sessions/feedbacks/me/pending")
    override fun getPendingFeedback(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<SessionFeedbackPendingResponse> =
        CustomResponse.ok(sessionFeedbackPendingQueryService.findPending(memberId))
}
