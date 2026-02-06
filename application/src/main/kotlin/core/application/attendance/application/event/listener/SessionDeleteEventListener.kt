package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionDeleteEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SessionDeleteEventListener(
    private val attendanceCommandService: AttendanceCommandService,
) {
    private val logger = KotlinLogging.logger { SessionDeleteEventListener::class.java }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleSessionDeletion(event: SessionDeleteEvent) {
        logger.info { "Handling SessionDeleteEvent for sessionId: ${event.sessionId}" }

        try {
            attendanceCommandService.deleteAttendancesBySessionId(
                sessionId = event.sessionId,
                deletedAt = event.deletedAt,
            )
        } catch (e: Exception) {
            logger.error(e) {
                "Error occurred while handling SessionDeleteEvent " +
                    "for sessionId: ${event.sessionId}. Caused By: ${e.cause}"
            }
        } finally {
            logger.info { "Completed handling SessionDeleteEvent for sessionId: ${event.sessionId}" }
        }
    }
}
