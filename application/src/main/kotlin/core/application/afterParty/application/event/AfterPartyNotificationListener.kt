package core.application.afterParty.application.event

import core.application.notification.application.service.NotificationCommandService
import core.domain.afterParty.event.AfterPartyCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AfterPartyNotificationListener(
    val notificationCommandService: NotificationCommandService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun createdEventHandle(afterPartyCreatedEvent: AfterPartyCreatedEvent) {
        notificationCommandService.sendPushNotificationToMembers(
            memberIds = afterPartyCreatedEvent.invitedMemberIds,
            messageType = afterPartyCreatedEvent.messageType,
            variables = mapOf("title" to afterPartyCreatedEvent.title),
            data = mapOf("afterPartyId" to afterPartyCreatedEvent.afterPartyId.value),
        )
    }
}
