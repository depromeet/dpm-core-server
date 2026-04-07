package core.domain.notification.port.inbound

import core.domain.notification.aggregate.SentSessionNotification

interface SentSessionNotificationCommandUseCase {
    fun updateSentAt(sentSessionNotification: SentSessionNotification): SentSessionNotification

    fun save(sentSessionNotification: SentSessionNotification): SentSessionNotification
}
