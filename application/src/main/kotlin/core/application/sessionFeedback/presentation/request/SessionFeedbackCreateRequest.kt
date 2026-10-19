package core.application.sessionFeedback.presentation.request

import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SessionFeedbackCreateRequest(
    @field:NotNull
    val satisfaction: SessionFeedbackSatisfaction?,
    @field:NotNull
    val likedAspects: List<@NotNull SessionFeedbackAspect>?,
    @field:Size(max = 100, message = "기타 의견은 100자 이하로 입력해주세요")
    val likedEtc: String? = null,
    @field:NotNull
    val improvementAspects: List<@NotNull SessionFeedbackAspect>?,
    @field:Size(max = 100, message = "기타 의견은 100자 이하로 입력해주세요")
    val improvementEtc: String? = null,
    @field:Size(max = 1000, message = "자유 의견은 1000자 이하로 입력해주세요")
    val freeComment: String? = null,
)
