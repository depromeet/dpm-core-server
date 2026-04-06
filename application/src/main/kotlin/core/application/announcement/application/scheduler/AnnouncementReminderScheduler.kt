package core.application.announcement.application.scheduler

import core.application.member.application.service.MemberQueryService
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.vo.AnnouncementId
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.notification.port.inbound.SentAnnouncementNotificationCommandUseCase
import core.domain.notification.port.inbound.SentAnnouncementNotificationQueryUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class AnnouncementReminderScheduler(
    val assignmentQueryUseCase: AssignmentQueryUseCase,
    val announcementQueryUseCase: AnnouncementQueryUseCase,
    val sentAnnouncementNotificationQueryUseCase: SentAnnouncementNotificationQueryUseCase,
    val sentAnnouncementNotificationCommandUseCase: SentAnnouncementNotificationCommandUseCase,
    val memberQueryService: MemberQueryService,
    val cohortQueryUseCase: CohortQueryUseCase,
    val notificationCommandUseCase: NotificationCommandUseCase,
) {
    @Scheduled(cron = "0 * * * * *")
    fun sendAssignmentDue24HReminder() {
        val now = Instant.now()

        checkAndSendReminders(now, Duration.ofHours(24), NotificationMessageType.ASSIGNMENT_DUE_24H)
    }

    @Scheduled(cron = "10 * * * * *")
    fun sendAssignmentDue12HReminder() {
        val now = Instant.now()

        checkAndSendReminders(now, Duration.ofHours(12), NotificationMessageType.ASSIGNMENT_DUE_12H)
    }

    @Scheduled(cron = "20 * * * * *")
    fun sendAssignmentDue1HReminder() {
        val now = Instant.now()

        checkAndSendReminders(now, Duration.ofHours(1), NotificationMessageType.ASSIGNMENT_DUE_1H)
    }

    @Transactional
    fun checkAndSendReminders(
        now: Instant,
        duration: Duration,
        messageType: NotificationMessageType,
    ) {
        val bufferTime: Duration = Duration.ofMinutes(5)
        val start = now.plus(duration).minus(bufferTime)
        val end = now.plus(duration).plus(bufferTime)

        val targetAssignments: List<Assignment> = assignmentQueryUseCase.findByDueAtBetween(start, end)

        if (targetAssignments.isEmpty()) return

        // TODO : 미제출 디퍼로 변경
        val currentCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        val targetMemberIds: List<MemberId> = memberQueryService.getMembersByCohortId(currentCohortId)

        // 각 assignment에 대해 발송 이력 확인 및 알림 발송
        targetAssignments.forEach { assignment ->
            val announcementId: AnnouncementId =
                announcementQueryUseCase.findAnnouncementByAssignmentId(assignment.id!!)?.id ?: return@forEach

            val sentNotifications: SentAnnouncementNotification =
                sentAnnouncementNotificationQueryUseCase
                    .findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
                        announcementId = announcementId,
                        notificationType = messageType,
                    ) ?: return@forEach
            if (sentNotifications.sentAt != null) return@forEach

            notificationCommandUseCase.sendPushNotificationToMembers(
                memberIds = targetMemberIds,
                messageType = messageType,
            )

            val updatedNotification =
                SentAnnouncementNotification(
                    sentAnnouncementNotificationId = sentNotifications.sentAnnouncementNotificationId,
                    announcementId = sentNotifications.announcementId,
                    notificationMessageType = sentNotifications.notificationMessageType,
                    sentAt = Instant.now(),
                )
            sentAnnouncementNotificationCommandUseCase.updateSentAt(updatedNotification)
        }
    }
}
