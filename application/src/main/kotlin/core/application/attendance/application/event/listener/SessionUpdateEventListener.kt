package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 세션 출석 시각 변경 후 출석 기록 재계산의 안전망.
 *
 * 재계산 자체는 세션 수정 트랜잭션 안에서 동기로 끝난다. 이 리스너는 커밋 이후 한 번 더 맞춰 보며,
 * 이벤트에 담긴 시각이 아니라 세션 행을 잠그고 읽은 최신 시각을 사용하므로
 * 늦게 실행되더라도 이후의 세션 변경을 오래된 값으로 덮어쓰지 않는다. 같은 규칙이라 여러 번 실행해도 결과가 같다.
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
