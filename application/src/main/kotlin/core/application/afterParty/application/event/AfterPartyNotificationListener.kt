package core.application.afterParty.application.event

import core.domain.afterParty.event.AfterPartyCreatedEvent
import core.domain.notification.port.inbound.NotificationCommandUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AfterPartyNotificationListener(
    val notificationCommandUseCase: NotificationCommandUseCase,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun createdEventHandle(afterPartyCreatedEvent: AfterPartyCreatedEvent) {
        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = afterPartyCreatedEvent.invitedMemberIds,
            messageType = afterPartyCreatedEvent.messageType,
            variables = mapOf("title" to afterPartyCreatedEvent.title),
            data = mapOf("afterPartyId" to afterPartyCreatedEvent.afterPartyId.value),
        )
    }
}
