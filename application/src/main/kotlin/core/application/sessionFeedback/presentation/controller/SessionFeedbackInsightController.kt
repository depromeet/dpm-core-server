package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.application.service.SessionFeedbackInsightQueryService
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse
import core.domain.session.vo.SessionId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionFeedbackInsightController(
    private val sessionFeedbackInsightQueryService: SessionFeedbackInsightQueryService,
) : SessionFeedbackInsightApi {
    @PreAuthorize("hasAuthority('update:session')")
    @GetMapping("/v2/sessions/{sessionId}/feedbacks/insight")
    override fun getInsight(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<SessionFeedbackInsightResponse> =
        CustomResponse.ok(sessionFeedbackInsightQueryService.getInsight(sessionId))
}
