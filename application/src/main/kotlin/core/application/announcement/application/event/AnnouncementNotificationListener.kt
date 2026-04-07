package core.application.announcement.application.event

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.application.service.AnnouncementQueryService
import core.application.member.application.service.MemberQueryService
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.event.AnnouncementCreatedEvent
import core.domain.announcement.event.AnnouncementRemindEvent
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnnouncementNotificationListener(
    val notificationCommandUseCase: NotificationCommandUseCase,
    val announcementQueryService: AnnouncementQueryService,
    val memberQueryService: MemberQueryService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun createdEventHandle(announcementCreatedEvent: AnnouncementCreatedEvent) {
        val messageType: NotificationMessageType =
            when (announcementCreatedEvent.announcementType) {
                AnnouncementType.GENERAL -> NotificationMessageType.ANNOUNCEMENT_NEW
                AnnouncementType.ASSIGNMENT -> NotificationMessageType.ASSIGNMENT_NEW
            }

        val memberIds: List<MemberId> =
            memberQueryService.getMemberIdsByCohortId(announcementCreatedEvent.cohortId)

        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = memberIds,
            messageType = messageType,
            variables = mapOf("title" to announcementCreatedEvent.title),
            data = mapOf("announcementId" to announcementCreatedEvent.announcementId.value),
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun createdEventHandle(announcementRemindEvent: AnnouncementRemindEvent) {
        val messageType: NotificationMessageType =
            when (announcementRemindEvent.announcement.announcementType) {
                AnnouncementType.GENERAL -> NotificationMessageType.ANNOUNCEMENT_REMIND
                AnnouncementType.ASSIGNMENT -> NotificationMessageType.ASSIGNMENT_SUBMIT_REQUEST
            }
        val announcementId: AnnouncementId =
            announcementRemindEvent.announcement.id ?: throw AnnouncementNotFoundException()

        val memberIds: List<MemberId> =
            when (announcementRemindEvent.announcement.announcementType) {
                AnnouncementType.GENERAL ->
                    announcementQueryService
                        .findUnreadByAnnouncementId(announcementId)
                        .map { it.memberId }
                AnnouncementType.ASSIGNMENT ->
                    announcementQueryService
                        .findUnsubmittedByAnnouncementIdAndSubmitStatus(announcementId)
                        .map { it.memberId }
            }

        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = memberIds,
            messageType = messageType,
            variables = mapOf("title" to announcementRemindEvent.announcement.title),
            data = mapOf("announcementId" to announcementRemindEvent.announcement.id!!.value),
        )
    }
}
