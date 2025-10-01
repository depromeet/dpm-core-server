package core.domain.session.vo

@JvmInline
value class SessionAttachmentId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
