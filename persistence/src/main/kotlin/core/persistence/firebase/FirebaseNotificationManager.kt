package core.persistence.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseNotificationManager {
    fun sendPushNotification(
        token: String,
        title: String,
        messageContent: String,
    ): Boolean {
        val notification: Notification =
            Notification.builder()
                .setTitle(title)
                .setBody(messageContent)
                .build()

        val message: Message =
            Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build()

        val send: String? =
            try {
                FirebaseMessaging.getInstance().send(message)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        return send != null
    }
}
