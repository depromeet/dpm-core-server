package core.application.announcement.application.scheduler

import core.application.member.application.service.MemberQueryService
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
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
    val sentAnnouncementNotificationQueryUseCase: SentAnnouncementNotificationQueryUseCase,
    val sentAnnouncementNotificationCommandUseCase: SentAnnouncementNotificationCommandUseCase,
    val memberQueryService: MemberQueryService,
    val cohortQueryUseCase: CohortQueryUseCase,
    val notificationCommandUseCase: NotificationCommandUseCase,
) {
    @Scheduled(cron = "0 * * * * *")
    fun sendAssignmentReminders() {
        val now = Instant.now()

        checkAndSendReminders(now, Duration.ofHours(24), NotificationMessageType.ASSIGNMENT_DUE_24H)
        checkAndSendReminders(now, Duration.ofHours(12), NotificationMessageType.ASSIGNMENT_DUE_12H)
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

        // due_at이 duration 범위 내에 있는 assignment 조회
        val targetAssignments: List<Assignment> = assignmentQueryUseCase.findByDueAtBetween(start, end)

        if (targetAssignments.isEmpty()) return

        // TODO : 미제출 디퍼로 변경
        val currentCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        val targetMemberIds: List<MemberId> = memberQueryService.getMembersByCohortId(currentCohortId)

        // 각 assignment에 대해 발송 이력 확인 및 알림 발송
        targetAssignments.forEach { assignment ->
            // 발송 이력 확인 (sentAt이 null인 레코드 찾기)
            val sentNotifications =
                sentAnnouncementNotificationQueryUseCase
                    .getSentAnnouncementNotificationByAssignmentIdAndNotificationType(
                        assignmentId = assignment.id!!,
                        notificationType = messageType,
                    )

            // sentAt이 null인 알림 레코드 찾기 (아직 발송되지 않은 알림)
            val unsentNotification = sentNotifications.firstOrNull { it.sentAt == null }

            // 미발송 알림이 있는 경우에만 알림 발송 및 발송 시간 업데이트
            if (unsentNotification != null) {
                // 알림 발송
                notificationCommandUseCase.sendPushNotificationToMembers(
                    memberIds = targetMemberIds,
                    messageType = messageType,
                )

                // 발송 시간 업데이트
                val updatedNotification =
                    SentAnnouncementNotification(
                        sentAnnouncementNotificationId = unsentNotification.sentAnnouncementNotificationId,
                        announcementId = unsentNotification.announcementId,
                        notificationMessageType = unsentNotification.notificationMessageType,
                        sentAt = Instant.now(),
                    )
                sentAnnouncementNotificationCommandUseCase.updateSentAt(updatedNotification)
            }
        }
    }
}
