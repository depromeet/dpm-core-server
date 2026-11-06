package core.application.notification.application.event

import core.application.notification.application.service.AbsenceReasonPushService
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.event.AbsenceReasonReviewedEvent
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AbsenceReasonNotificationListener(
    private val memberRoles: MemberRolePersistencePort,
    private val pushes: AbsenceReasonPushService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSubmitted(event: AbsenceReasonSubmittedEvent) {
        pushes.send(
            recipients = memberRoles.findAttendanceStaffIds(event.cohortId),
            type = NotificationMessageType.ABSENCE_REASON_SUBMITTED,
            variables = mapOf("name" to event.submitterName, "week" to event.week),
            data = mapOf("sessionId" to event.sessionId.value, "memberId" to event.memberId.value),
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReviewed(event: AbsenceReasonReviewedEvent) {
        val type =
            if (event.approved) {
                NotificationMessageType.ABSENCE_REASON_APPROVED
            } else {
                NotificationMessageType.ABSENCE_REASON_REJECTED
            }
        pushes.send(
            recipients = listOf(event.memberId),
            type = type,
            variables = mapOf("week" to event.week),
            data = mapOf("sessionId" to event.sessionId.value),
        )
    }
}
