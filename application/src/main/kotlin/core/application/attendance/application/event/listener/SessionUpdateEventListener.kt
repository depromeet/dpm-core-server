package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 재계산은 세션 수정 트랜잭션에서 이미 끝난다. 이 리스너는 안전망으로, 이벤트 값 대신 잠금 후 읽은 최신 시각을 써서
 * 늦게 실행돼도 이후 변경을 덮어쓰지 않는다.
 */
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
            attendanceCommandService.reconcileAttendancesWithLatestPolicy(event.sessionId)
        } catch (e: Exception) {
            logger.error(e) {
                "Error occurred while handling SessionUpdateEvent " +
                    "for sessionId: ${event.sessionId}. Caused By: ${e.cause}"
            }
        } finally {
            logger.info { "Completed handling SessionUpdateEvent for sessionId: ${event.sessionId}" }
        }
    }
}
