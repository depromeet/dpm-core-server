package core.domain.session.vo

@JvmInline
value class SessionId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
