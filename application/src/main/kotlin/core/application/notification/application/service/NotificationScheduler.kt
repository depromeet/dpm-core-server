package core.application.notification.application.service

import core.application.member.application.service.MemberQueryService
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.event.InviteTagNotificationEvent
import core.domain.notification.event.NotificationByMemberEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationScheduler(
    val notificationCommandService: NotificationCommandService,
    val memberQueryUseCase: MemberQueryService,
    val cohortQueryUseCase: CohortQueryUseCase,
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

        notificationCommandService.sendPushNotificationToMembers(
            memberIds = inviteeMemberIds,
            messageType = inviteTagNotificationEvent.notificationMessage,
        )
    }
}
