package core.application.notification.application.service

import core.persistence.firebase.FirebaseNotificationManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class NotificationService(
    val firebaseNotificationManager: FirebaseNotificationManager,
) {
    @Async
    fun sendPushNotification(
        token: String,
        title: String,
        messageContent: String,
    ): Boolean = firebaseNotificationManager.sendPushNotification(token, title, messageContent)
}
