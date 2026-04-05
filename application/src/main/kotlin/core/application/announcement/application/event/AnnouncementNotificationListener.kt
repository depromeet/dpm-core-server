package core.application.announcement.application.event

import core.application.member.application.service.MemberQueryService
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.event.AnnouncementCreatedEvent
import core.domain.cohort.vo.AuthorityId
import core.domain.member.constant.AuthorityConstants.DEEPER_AUTHORITY_ID
import core.domain.notification.enums.NotificationMessage
import core.domain.notification.port.inbound.NotificationCommandUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnnouncementNotificationListener(
    val notificationCommandUseCase: NotificationCommandUseCase,
    val memberQueryService: MemberQueryService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun createdEventHandle(announcementCreatedEvent: AnnouncementCreatedEvent) {
        val messageType: NotificationMessage =
            when (announcementCreatedEvent.announcementType) {
                AnnouncementType.GENERAL -> NotificationMessage.ANNOUNCEMENT_NEW
                AnnouncementType.ASSIGNMENT -> NotificationMessage.ASSIGNMENT_NEW
            }

        val memberIds =
            memberQueryService.findAllMemberIdsByCohortIdAndAuthorityId(
                cohortId = announcementCreatedEvent.cohortId,
                authorityId = AuthorityId(DEEPER_AUTHORITY_ID),
            )

        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = memberIds,
            messageType = messageType,
            variables = mapOf("title" to announcementCreatedEvent.title),
            data = mapOf("announcementId" to announcementCreatedEvent.announcementId.value),
        )
    }
}
