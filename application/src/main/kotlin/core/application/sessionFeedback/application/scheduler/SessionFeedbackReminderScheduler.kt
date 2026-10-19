package core.application.sessionFeedback.application.scheduler

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class SessionFeedbackReminderScheduler(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val notificationCommandUseCase: NotificationCommandUseCase,
    private val clock: Clock,
) {
    @Scheduled(cron = "10 0/5 * * * *")
    @Transactional
    fun sendFeedbackOpenedPush() {
        val now = clock.instant()
        val pendingForms = feedbackFormPersistencePort.findAllPendingPushAt(now)
        if (pendingForms.isEmpty()) return

        pendingForms.forEach { form -> sendOpenedPushForForm(form, now) }
    }

    private fun sendOpenedPushForForm(
        form: SessionFeedbackForm,
        now: java.time.Instant,
    ) {
        val sessionId = form.sessionId.value
        val session = sessionPersistencePort.findSessionById(sessionId) ?: return

        val targetMemberIds = findTargetMemberIds(sessionId)
        if (targetMemberIds.isNotEmpty()) {
            notificationCommandUseCase.sendPushNotificationToMembers(
                memberIds = targetMemberIds,
                messageType = NotificationMessageType.SESSION_FEEDBACK_OPENED,
                variables = mapOf("title" to session.eventName),
                data = mapOf("sessionId" to sessionId),
            )
        }

        form.markPushSent(now)
        feedbackFormPersistencePort.save(form)
    }

    private fun findTargetMemberIds(sessionId: Long): List<MemberId> =
        attendancePersistencePort
            .findAllBySessionId(sessionId)
            .asSequence()
            .filter { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            .map { it.memberId }
            .toList()
}
