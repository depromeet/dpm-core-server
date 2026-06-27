package core.application.notification.application.event

import core.application.member.application.service.MemberQueryService
import core.application.notification.application.service.NotificationCommandService
import core.domain.cohort.vo.AuthorityId
import core.domain.member.aggregate.InviteTagSpec
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.event.AbsenceReasonSubmittedEvent
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
    companion object {
        /** 운영진 권한 식별자 (레거시 authorities 테이블 기준) */
        private val ORGANIZER_AUTHORITY_ID = AuthorityId(2L)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendScheduledNotificationsByMemberId(notificationByMemberEvent: NotificationByMemberEvent) {
        notificationCommandService.sendPushNotification(
            memberId = notificationByMemberEvent.memberId,
            messageType = notificationByMemberEvent.notificationMessageType,
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
            messageType = inviteTagNotificationEvent.notificationMessageType,
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendAbsenceReasonSubmittedNotification(event: AbsenceReasonSubmittedEvent) {
        notificationCommandService.sendPushNotificationByTags(
            tags = listOf(InviteTagSpec.of(event.cohortId, ORGANIZER_AUTHORITY_ID)),
            messageType = NotificationMessageType.ABSENCE_REASON_SUBMITTED,
            variables =
                mapOf(
                    "name" to event.submitterName,
                    "week" to event.week,
                ),
            data =
                mapOf(
                    "type" to "ABSENCE_REASON",
                    "sessionId" to event.sessionId.value,
                    "memberId" to event.memberId.value,
                ),
        )
    }
}
