package core.application.session.application.scheduler

import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.notification.port.inbound.SentSessionNotificationQueryUseCase
import core.domain.notification.vo.SentSessionNotificationId
import core.domain.session.port.outbound.SessionPersistencePort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class SessionReminderScheduler(
    val cohortQueryUseCase: CohortQueryUseCase,
    val memberQueryUseCase: MemberQueryUseCase,
    val notificationCommandUseCase: NotificationCommandUseCase,
    val sessionPersistencePort: SessionPersistencePort,
    val sentSessionNotificationQueryUseCase: SentSessionNotificationQueryUseCase,
    val sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase,
) {
    @Scheduled(cron = "0 0/10 * * * *")
    @Transactional
    fun sendStartAttendanceReminder() {
        val currentCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        val currentCohortMemberIds: List<MemberId> = memberQueryUseCase.getMemberIdsByCohortId(currentCohortId)

        val now = Instant.now()
        val bufferTime: Duration = Duration.ofMinutes(15)
        val startTime = now.minus(bufferTime)

        val sessionsToNotify =
            sessionPersistencePort.findSessionsWithAttendanceStartTimeBetween(
                cohortId = currentCohortId,
                startTime = startTime,
                endTime = now,
            )

        if (sessionsToNotify.isEmpty()) {
            return
        }

        val sessionIds = sessionsToNotify.mapNotNull { it.id }
        val sentNotificationsMap =
            sentSessionNotificationQueryUseCase
                .findSentSessionNotificationsBySessionIdsAndNotificationType(
                    sessionIds = sessionIds,
                    notificationType = NotificationMessageType.ATTENDANCE_STARTED,
                ).associateBy { it.sessionId }

        sessionsToNotify.forEach { session ->
            val sessionId = session.id ?: return@forEach

            val sentNotification: SentSessionNotification? = sentNotificationsMap[sessionId]

            if (sentNotification?.sentAt != null) return@forEach

            notificationCommandUseCase.sendPushNotificationToMembers(
                memberIds = currentCohortMemberIds,
                messageType = NotificationMessageType.ATTENDANCE_STARTED,
            )

            if (sentNotification != null) {
                val updatedNotification =
                    SentSessionNotification(
                        sentSessionNotificationId = sentNotification.sentSessionNotificationId,
                        sessionId = sentNotification.sessionId,
                        notificationMessageType = sentNotification.notificationMessageType,
                        sentAt = Instant.now(),
                    )
                sentSessionNotificationCommandUseCase.updateSentAt(updatedNotification)
            } else {
                val newNotification =
                    SentSessionNotification(
                        sentSessionNotificationId = SentSessionNotificationId(0L),
                        sessionId = sessionId,
                        notificationMessageType = NotificationMessageType.ATTENDANCE_STARTED,
                        sentAt = Instant.now(),
                    )
                sentSessionNotificationCommandUseCase.save(newNotification)
            }
        }
    }

    @Scheduled(cron = "0 0/5 14 * * *")
    @Transactional
    fun sendNextDaySessionReminder() {
        val currentCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        val currentCohortMemberIds: List<MemberId> = memberQueryUseCase.getMemberIdsByCohortId(currentCohortId)

        val now = Instant.now()
        val bufferTime: Duration = Duration.ofHours(30)
        val endTime = now.plus(bufferTime)

        val sessionsToNotify =
            sessionPersistencePort.findSessionsWithAttendanceStartTimeBetween(
                cohortId = currentCohortId,
                startTime = now,
                endTime = endTime,
            )

        if (sessionsToNotify.isEmpty()) {
            return
        }

        val sessionIds = sessionsToNotify.mapNotNull { it.id }
        val sentNotificationsMap =
            sentSessionNotificationQueryUseCase
                .findSentSessionNotificationsBySessionIdsAndNotificationType(
                    sessionIds = sessionIds,
                    notificationType = NotificationMessageType.SESSION_DAY_BEFORE,
                ).associateBy { it.sessionId }

        sessionsToNotify.forEach { session ->
            val sessionId = session.id ?: return@forEach

            val sentNotification: SentSessionNotification? = sentNotificationsMap[sessionId]

            if (sentNotification?.sentAt != null) return@forEach

            notificationCommandUseCase.sendPushNotificationToMembers(
                memberIds = currentCohortMemberIds,
                messageType = NotificationMessageType.SESSION_DAY_BEFORE,
                variables = mapOf("title" to session.eventName),
                data = mapOf("sessionId" to session.id!!.value),
            )

            if (sentNotification != null) {
                val updatedNotification =
                    SentSessionNotification(
                        sentSessionNotificationId = sentNotification.sentSessionNotificationId,
                        sessionId = sentNotification.sessionId,
                        notificationMessageType = sentNotification.notificationMessageType,
                        sentAt = Instant.now(),
                    )
                sentSessionNotificationCommandUseCase.updateSentAt(updatedNotification)
            } else {
                val newNotification =
                    SentSessionNotification(
                        sentSessionNotificationId = SentSessionNotificationId(0L),
                        sessionId = sessionId,
                        notificationMessageType = NotificationMessageType.SESSION_DAY_BEFORE,
                        sentAt = Instant.now(),
                    )
                sentSessionNotificationCommandUseCase.save(newNotification)
            }
        }
    }
}
