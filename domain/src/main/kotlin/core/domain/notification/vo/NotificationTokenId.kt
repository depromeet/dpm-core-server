package core.domain.notification.vo

@JvmInline
value class NotificationTokenId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
