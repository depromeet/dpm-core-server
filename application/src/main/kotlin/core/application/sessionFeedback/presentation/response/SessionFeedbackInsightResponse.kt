package core.application.sessionFeedback.presentation.response

import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import java.time.LocalDateTime

/**
 * 운영진 피드백 인사이트 응답.
 *
 * 응답자 식별 정보(memberId·이름·팀)는 포함하지 않는다. 실시간 집계라 수집 중에도 조회 가능하다.
 * 수집 전·응답 0건은 에러가 아니라 200 + `respondentCount=0` 으로 내려주고, FE 가 빈 상태 화면을 보여준다.
 */
data class SessionFeedbackInsightResponse(
    val sessionId: Long,
    val sessionName: String,
    val status: SessionFeedbackStatus,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val responseSummary: ResponseSummary,
    val satisfaction: SatisfactionInsight,
    val likedAspects: AspectsInsight,
    val improvementAspects: AspectsInsight,
    val freeComments: FreeCommentsInsight,
) {
    data class ResponseSummary(
        val respondentCount: Int,
        val targetCount: Int,
        val responseRate: Int,
    )

    data class SatisfactionInsight(
        val average: Double,
        val maxScore: Int,
        val distribution: List<SatisfactionDistributionItem>,
    )

    data class SatisfactionDistributionItem(
        val code: SessionFeedbackSatisfaction,
        val label: String,
        val count: Int,
        val rate: Int,
    )

    data class AspectsInsight(
        val totalSelectionCount: Int,
        val items: List<AspectInsightItem>,
        val etcComments: List<String>,
    )

    data class AspectInsightItem(
        val code: SessionFeedbackAspect,
        val label: String,
        val count: Int,
        val rate: Int,
    )

    data class FreeCommentsInsight(
        val count: Int,
        val items: List<String>,
    )

    companion object {
        const val MAX_SCORE: Int = 5

        val SATISFACTION_INSIGHT_LABELS: Map<SessionFeedbackSatisfaction, String> =
            mapOf(
                SessionFeedbackSatisfaction.VERY_SATISFIED to "매우 만족",
                SessionFeedbackSatisfaction.SATISFIED to "만족",
                SessionFeedbackSatisfaction.NEUTRAL to "보통",
                SessionFeedbackSatisfaction.DISSATISFIED to "불만족",
                SessionFeedbackSatisfaction.VERY_DISSATISFIED to "매우 불만족",
            )

        val ASPECT_LABELS: Map<SessionFeedbackAspect, String> =
            mapOf(
                SessionFeedbackAspect.SESSION_CONTENT to "세션 내용",
                SessionFeedbackAspect.PROGRESS_AND_TIME to "진행 방식·시간",
                SessionFeedbackAspect.NETWORKING to "교류 기회",
                SessionFeedbackAspect.GUIDANCE to "사전·현장 안내",
                SessionFeedbackAspect.PLACE_AND_ACCESS to "장소·접속 환경",
                SessionFeedbackAspect.ETC to "기타",
                SessionFeedbackAspect.NOTHING to "특별히 없음",
            )
    }
}
