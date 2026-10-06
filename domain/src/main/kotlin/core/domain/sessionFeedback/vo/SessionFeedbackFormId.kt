package core.domain.sessionFeedback.vo

@JvmInline
value class SessionFeedbackFormId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
