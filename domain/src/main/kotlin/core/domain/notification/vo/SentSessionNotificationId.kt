package core.domain.notification.vo

@JvmInline
value class SentSessionNotificationId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
