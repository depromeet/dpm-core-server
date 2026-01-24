package core.application.notification.application.service

import core.application.notification.application.exception.NotificationFailedException
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
    ) {
        val isNotificationSuccess: Boolean =
            firebaseNotificationManager.sendPushNotification(
                token,
                title,
                messageContent,
            )
        if (!isNotificationSuccess) {
            throw NotificationFailedException()
        }
    }
}
