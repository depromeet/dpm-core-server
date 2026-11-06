package core.application.sessionFeedback.application.scheduler

import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.aggregate.SessionFeedbackPushReceipt
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPushReceiptPersistencePort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant

@Component
class SessionFeedbackReminderScheduler(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val pushReceiptPersistencePort: SessionFeedbackPushReceiptPersistencePort,
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val notificationCommandUseCase: NotificationCommandUseCase,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    @Scheduled(cron = "10 0/5 * * * *")
    fun sendFeedbackOpenedPush() {
        val now = clock.instant()
        val pendingForms = feedbackFormPersistencePort.findAllPendingPushAt(now)
        if (pendingForms.isEmpty()) return

        pendingForms.forEach { form ->
            transactionTemplate.executeWithoutResult { sendOpenedPushForForm(form, now) }
        }
    }

    private fun sendOpenedPushForForm(
        form: SessionFeedbackForm,
        now: Instant,
    ) {
        val sessionId = form.sessionId.value
        val session = sessionPersistencePort.findSessionById(sessionId) ?: return

        val eligible = findEligibleAttendances(sessionId)
        val alreadySent = pushReceiptPersistencePort.findMemberIdsBySessionId(sessionId).toMutableSet()

        // 예전 일괄 발송은 출석·지각자만 받았다. 늦은 참여 등록자는 실제 발송 이력으로만 판단한다.
        val pushSentAt = form.pushSentAt
        if (pushSentAt != null) {
            val legacy =
                eligible.filter { attendance ->
                    attendance.memberId !in alreadySent &&
                        attendance.lateParticipatedAt == null &&
                        (attendance.attendedAt == null || !attendance.attendedAt!!.isAfter(pushSentAt))
                }
            if (legacy.isNotEmpty()) {
                pushReceiptPersistencePort.saveAll(
                    legacy.map {
                        SessionFeedbackPushReceipt.create(form.sessionId, it.memberId, pushSentAt)
                    },
                )
                alreadySent += legacy.map { it.memberId }
            }
        }

        val targets = eligible.map { it.memberId }.filterNot { it in alreadySent }
        if (targets.isEmpty()) return

        // 비동기 푸시보다 이력을 먼저 남겨, 다음 틱/다른 인스턴스에서 같은 멤버를 다시 고르지 않게 한다.
        pushReceiptPersistencePort.saveAll(
            targets.map { SessionFeedbackPushReceipt.create(SessionId(sessionId), it, now) },
        )
        if (form.pushSentAt == null) {
            form.markPushSent(now)
            feedbackFormPersistencePort.save(form)
        }

        notificationCommandUseCase.sendPushNotificationToMembers(
            memberIds = targets,
            messageType = NotificationMessageType.SESSION_FEEDBACK_OPENED,
            variables = mapOf("title" to session.eventName),
            data = mapOf("sessionId" to sessionId),
        )
    }

    private fun findEligibleAttendances(sessionId: Long) =
        attendancePersistencePort
            .findAllBySessionId(sessionId)
            .filter { it.isSessionFeedbackEligible() }
}
