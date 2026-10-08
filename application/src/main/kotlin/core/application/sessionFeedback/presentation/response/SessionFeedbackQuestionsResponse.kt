package core.application.sessionFeedback.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction

data class SessionFeedbackQuestionsResponse(
    val satisfaction: List<SatisfactionOption>,
    val likedAspects: List<AspectOption>,
    val improvementAspects: List<AspectOption>,
) {
    data class SatisfactionOption(
        val code: SessionFeedbackSatisfaction,
        val score: Int,
        val label: String,
    )

    data class AspectOption(
        val code: SessionFeedbackAspect,
        val label: String,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        val requiresText: Boolean = false,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        val exclusive: Boolean = false,
    )

    companion object {
        private val SATISFACTION_LABELS: Map<SessionFeedbackSatisfaction, String> =
            mapOf(
                SessionFeedbackSatisfaction.VERY_SATISFIED to "매우 만족했어요.",
                SessionFeedbackSatisfaction.SATISFIED to "만족했어요.",
                SessionFeedbackSatisfaction.NEUTRAL to "보통이었어요.",
                SessionFeedbackSatisfaction.DISSATISFIED to "별로였어요.",
                SessionFeedbackSatisfaction.VERY_DISSATISFIED to "매우 별로였어요.",
            )

        private val ASPECT_LABELS: Map<SessionFeedbackAspect, String> =
            mapOf(
                SessionFeedbackAspect.SESSION_CONTENT to "세션 내용",
                SessionFeedbackAspect.PROGRESS_AND_TIME to "진행 방식·시간",
                SessionFeedbackAspect.NETWORKING to "교류 기회",
                SessionFeedbackAspect.GUIDANCE to "사전·현장 안내",
                SessionFeedbackAspect.PLACE_AND_ACCESS to "장소·접속 환경",
                SessionFeedbackAspect.ETC to "기타",
                SessionFeedbackAspect.NOTHING to "특별히 없음",
            )

        private val SATISFACTION_OPTIONS: List<SatisfactionOption> =
            SessionFeedbackSatisfaction.entries
                .sortedByDescending { it.score }
                .map { code ->
                    SatisfactionOption(
                        code = code,
                        score = code.score,
                        label = SATISFACTION_LABELS.getValue(code),
                    )
                }

        private val ASPECT_OPTIONS: List<AspectOption> =
            SessionFeedbackAspect.entries.map { code ->
                AspectOption(
                    code = code,
                    label = ASPECT_LABELS.getValue(code),
                    requiresText = code.requiresText,
                    exclusive = code.exclusive,
                )
            }

        val DEFAULT: SessionFeedbackQuestionsResponse =
            SessionFeedbackQuestionsResponse(
                satisfaction = SATISFACTION_OPTIONS,
                likedAspects = ASPECT_OPTIONS,
                improvementAspects = ASPECT_OPTIONS,
            )
    }
}
