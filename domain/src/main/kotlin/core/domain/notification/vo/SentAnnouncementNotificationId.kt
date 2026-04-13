package core.domain.notification.vo

@JvmInline
value class SentAnnouncementNotificationId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
