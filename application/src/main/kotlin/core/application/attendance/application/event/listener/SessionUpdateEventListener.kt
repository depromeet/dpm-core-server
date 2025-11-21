package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SessionUpdateEventListener(
    private val attendanceCommandService: AttendanceCommandService,
) {
    private val logger = KotlinLogging.logger { SessionUpdateEventListener::class.java }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleSessionUpdate(event: SessionUpdateEvent) {
        logger.info { "Handling SessionUpdateEvent for sessionId: ${event.sessionId}" }

        try {
            attendanceCommandService.updateAttendancesByPolicy(
                sessionId = event.sessionId,
                lateStart = event.lateStart,
                absentStart = event.absentStart,
            )
        } catch (e: Exception) {
            logger.error(e) {
                "Error occurred while handling SessionUpdateEvent for sessionId: ${event.sessionId}. Caused By: ${e.cause}"
            }
        } finally {
            logger.info { "Completed handling SessionUpdateEvent for sessionId: ${event.sessionId}" }
        }
    }
}
