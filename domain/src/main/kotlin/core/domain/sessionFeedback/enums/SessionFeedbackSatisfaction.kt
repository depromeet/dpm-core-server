package core.domain.sessionFeedback.enums

/**
 * 세션 피드백 만족도.
 *
 * score 는 Figma 라벨 기준 1~5 점수이며, 인사이트 평균 계산에 사용된다.
 */
enum class SessionFeedbackSatisfaction(
    val score: Int,
) {
    VERY_SATISFIED(5),
    SATISFIED(4),
    NEUTRAL(3),
    DISSATISFIED(2),
    VERY_DISSATISFIED(1),
    ;
}
