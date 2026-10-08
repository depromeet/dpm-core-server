package core.domain.sessionFeedback.vo

@JvmInline
value class SessionFeedbackId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
