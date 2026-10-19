package core.domain.sessionFeedback.enums

enum class SessionFeedbackAspect(
    val requiresText: Boolean = false,
    val exclusive: Boolean = false,
) {
    SESSION_CONTENT,
    PROGRESS_AND_TIME,
    NETWORKING,
    GUIDANCE,
    PLACE_AND_ACCESS,
    ETC(requiresText = true),
    NOTHING(exclusive = true),
    ;
}
