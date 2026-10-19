package core.domain.sessionFeedback.vo

@JvmInline
value class SessionFeedbackAspectId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
