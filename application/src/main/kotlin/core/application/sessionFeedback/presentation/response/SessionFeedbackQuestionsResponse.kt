package core.application.sessionFeedback.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction

data class SessionFeedbackQuestionsResponse(
    val satisfaction: SatisfactionQuestion,
    val likedAspects: AspectQuestion,
    val improvementAspects: AspectQuestion,
    val freeComment: FreeCommentQuestion,
) {
    data class SatisfactionQuestion(
        val title: String,
        val required: Boolean = true,
        val options: List<SatisfactionOption>,
    )

    data class SatisfactionOption(
        val code: SessionFeedbackSatisfaction,
        val score: Int,
        val label: String,
    )

    data class AspectQuestion(
        val title: String,
        val description: String,
        val required: Boolean = true,
        val maxSelect: Int = 2,
        val options: List<AspectOption>,
        val etcPlaceholder: String,
    )

    data class AspectOption(
        val code: SessionFeedbackAspect,
        val label: String,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        val requiresText: Boolean = false,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        val exclusive: Boolean = false,
    )

    data class FreeCommentQuestion(
        val title: String,
        val placeholder: String,
        val required: Boolean = false,
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

        private const val ASPECT_DESCRIPTION = "최대 2개까지 선택해주세요."
        private const val LIKED_ETC_PLACEHOLDER = "어떤 부분이 좋았는지 적어주세요."
        private const val IMPROVEMENT_ETC_PLACEHOLDER = "어떤 부분의 개선이 필요한지 적어주세요."
        private const val FREE_COMMENT_TITLE = "세션에 대해 더 전하고 싶은 이야기가 있나요?"
        private const val FREE_COMMENT_PLACEHOLDER =
            "앞에서 고른 항목의 이유나 그 밖의 의견을 자유롭게 남겨주세요. (선택)"

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

        fun forSession(sessionName: String): SessionFeedbackQuestionsResponse =
            SessionFeedbackQuestionsResponse(
                satisfaction =
                    SatisfactionQuestion(
                        title = "$sessionName\n이번 세션에 얼마나 만족하셨나요?",
                        options = SATISFACTION_OPTIONS,
                    ),
                likedAspects =
                    AspectQuestion(
                        title = "이번 세션에서 특히 좋았던 부분이 있었나요?",
                        description = ASPECT_DESCRIPTION,
                        options = ASPECT_OPTIONS,
                        etcPlaceholder = LIKED_ETC_PLACEHOLDER,
                    ),
                improvementAspects =
                    AspectQuestion(
                        title = "이번 세션에서 개선이 필요한 부분이 있었나요?",
                        description = ASPECT_DESCRIPTION,
                        options = ASPECT_OPTIONS,
                        etcPlaceholder = IMPROVEMENT_ETC_PLACEHOLDER,
                    ),
                freeComment =
                    FreeCommentQuestion(
                        title = FREE_COMMENT_TITLE,
                        placeholder = FREE_COMMENT_PLACEHOLDER,
                    ),
            )
    }
}
