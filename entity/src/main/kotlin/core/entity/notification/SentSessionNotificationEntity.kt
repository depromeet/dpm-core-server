package core.entity.notification

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.vo.SentSessionNotificationId
import core.domain.session.vo.SessionId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "sent_session_notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_sent_session_notification",
            columnNames = ["session_id", "notification_message"],
        ),
    ],
)
class SentSessionNotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sent_session_notification_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "session_id", nullable = false)
    val sessionId: Long,
    @Column(name = "notification_message", nullable = false, length = 30)
    val notificationMessageType: String,
    @Column(name = "sent_at")
    val sentAt: Instant? = null,
) {
    companion object {
        fun from(sentSessionNotification: SentSessionNotification): SentSessionNotificationEntity =
            SentSessionNotificationEntity(
                id = sentSessionNotification.sentSessionNotificationId.value,
                sessionId = sentSessionNotification.sessionId.value,
                notificationMessageType = sentSessionNotification.notificationMessageType.name,
                sentAt = sentSessionNotification.sentAt,
            )
    }

    fun toDomain(): SentSessionNotification =
        SentSessionNotification(
            sentSessionNotificationId = SentSessionNotificationId(id),
            sessionId = SessionId(sessionId),
            notificationMessageType = NotificationMessageType.valueOf(notificationMessageType),
            sentAt = sentAt,
        )
}
