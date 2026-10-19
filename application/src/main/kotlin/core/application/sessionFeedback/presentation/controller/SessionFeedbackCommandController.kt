package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.BusinessException
import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.security.annotation.CurrentMemberId
import core.application.sessionFeedback.application.service.SessionFeedbackCommandService
import core.application.sessionFeedback.presentation.request.SessionFeedbackCreateRequest
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionFeedbackCommandController(
    private val sessionFeedbackCommandService: SessionFeedbackCommandService,
) : SessionFeedbackCommandApi {
    @PreAuthorize("hasAuthority('read:session')")
    @PostMapping("/v1/sessions/{sessionId}/feedbacks")
    override fun submitFeedback(
        @PathVariable(name = "sessionId") sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
        @Valid @RequestBody request: SessionFeedbackCreateRequest,
    ): CustomResponse<Void> {
        sessionFeedbackCommandService.submit(
            sessionId = sessionId,
            memberId = memberId,
            satisfaction = requireNotNull(request.satisfaction),
            likedAspects = requireNonNullAspects(request.likedAspects, "likedAspects"),
            improvementAspects = requireNonNullAspects(request.improvementAspects, "improvementAspects"),
            likedEtc = request.likedEtc,
            improvementEtc = request.improvementEtc,
            freeComment = request.freeComment?.takeIf { it.isNotBlank() },
        )
        return CustomResponse.ok()
    }

    private fun requireNonNullAspects(
        aspects: List<SessionFeedbackAspect?>?,
        fieldName: String,
    ): List<SessionFeedbackAspect> {
        val values = requireNotNull(aspects) { "$fieldName: 필수 입력값입니다" }
        if (values.any { it == null }) {
            throw BusinessException(GlobalExceptionCode.INVALID_INPUT)
        }
        @Suppress("UNCHECKED_CAST")
        return values as List<SessionFeedbackAspect>
    }
}
