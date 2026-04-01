package core.domain.notification.aggregate

data class NotificationExpoMessage(
    val to: String,
    val title: String,
    val body: String,
    val data: Map<String, Any>? = null,
    val priority: String,
    val sound: String,
    val badge: Int? = null,
    val channelId: String? = null,
)
