package core.application.notification.application.event

import core.application.member.application.service.MemberQueryService
import core.application.notification.application.service.NotificationCommandService
import core.domain.member.vo.MemberId
import core.domain.notification.event.InviteTagNotificationEvent
import core.domain.notification.event.NotificationByMemberEvent
import core.domain.notification.port.inbound.NotificationCommandUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationListener(
    val notificationCommandUseCase: NotificationCommandUseCase,
    val notificationCommandService: NotificationCommandService,
    val memberQueryUseCase: MemberQueryService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendScheduledNotificationsByMemberId(notificationByMemberEvent: NotificationByMemberEvent) {
        notificationCommandService.sendPushNotification(
            memberId = notificationByMemberEvent.memberId,
            messageType = notificationByMemberEvent.notificationMessage,
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendScheduledNotifications(inviteTagNotificationEvent: InviteTagNotificationEvent) {
        val inviteeMemberIds: List<MemberId> =
            inviteTagNotificationEvent.inviteTags
                .flatMap { tag ->
                    memberQueryUseCase.findAllMemberIdsByCohortIdAndAuthorityId(
                        tag.cohortId,
                        tag.authorityId,
                    )
                }.distinct()

        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = inviteeMemberIds,
            messageType = inviteTagNotificationEvent.notificationMessage,
        )
    }
}
