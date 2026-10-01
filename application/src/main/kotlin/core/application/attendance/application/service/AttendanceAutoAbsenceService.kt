package core.application.attendance.application.service

import core.domain.session.port.outbound.SessionPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 인증 마감이 지난 세션의 미인증 출석을 자동 결석으로 바꾼다.
 *
 * - 대상: 모든 기수(과거 기수 포함)의 삭제되지 않은 세션 중 인증 마감 <= 현재 시각이고 미인증 기록이 남은 세션.
 *   하한이나 짧은 lookback 이 없으므로 서버가 멈췄다 재시작해도 그동안 지난 마감을 모두 처리한다.
 * - 미인증(PENDING, 인증/운영진 변경 없음) 기록만 바꾼다. 출석/지각/인정 결석/운영진 변경 기록은 그대로다.
 * - 세션마다 별도 트랜잭션에서 세션 잠금을 잡고 최신 마감을 다시 확인한 뒤 처리하므로,
 *   한 세션의 실패가 다른 세션 처리를 막지 않고 그 사이 연장/삭제된 세션은 건너뛴다.
 * - 조건부 UPDATE 라 여러 번(또는 여러 서버에서) 실행돼도 결과가 같다.
 *
 * 트랜잭션은 각 단계가 호출하는 서비스가 연다. 이 클래스 자체는 트랜잭션을 열지 않는다.
 */
@Service
class AttendanceAutoAbsenceService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendanceCommandService: AttendanceCommandService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { AttendanceAutoAbsenceService::class.java }

    /** @return 이번 실행에서 자동 결석 처리한 출석 기록 수 */
    fun closeExpiredAttendances(): Int {
        val now = clock.instant()
        val sessionIds = sessionPersistencePort.findSessionIdsToAutoClose(absentStartTo = now)

        var closedCount = 0
        sessionIds.forEach { sessionId ->
            try {
                val closed = attendanceCommandService.closeExpiredAttendances(sessionId = sessionId, now = now)
                if (closed > 0) {
                    logger.info { "Auto absence: sessionId=$sessionId, closed=$closed" }
                }
                closedCount += closed
            } catch (e: Exception) {
                logger.error(e) { "Auto absence failed for sessionId=$sessionId" }
            }
        }

        return closedCount
    }
}
