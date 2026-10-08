package core.domain.sessionFeedback.enums

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
