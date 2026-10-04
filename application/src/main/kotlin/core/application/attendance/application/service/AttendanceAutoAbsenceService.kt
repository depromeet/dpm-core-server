package core.application.attendance.application.service

import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 마감이 지난 모든 기수(과거 포함, 하한 없음) 세션의 미인증 출석을 자동 결석으로 바꾼다.
 * 세션마다 별도 트랜잭션으로 처리해 한 세션의 실패가 다른 세션을 막지 않는다. 조건부 UPDATE 라 반복 실행해도 같다.
 * 실패한 세션만 짧게 쉬었다가 최대 [MAX_ATTEMPTS]번까지 다시 시도한다. 대상 조회 자체가 실패하면 다시 시도하지 않고 다음 실행을 기다린다.
 */
@Service
class AttendanceAutoAbsenceService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendanceCommandService: AttendanceCommandService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { AttendanceAutoAbsenceService::class.java }

    fun closeExpiredAttendances(): Int {
        val now = clock.instant()
        var pending = sessionPersistencePort.findSessionIdsToAutoClose(absentStartTo = now)

        var closedCount = 0
        for (attempt in 1..MAX_ATTEMPTS) {
            if (pending.isEmpty()) break
            if (attempt > 1) waitBeforeRetry(RETRY_DELAY)

            val failed = mutableListOf<SessionId>()
            pending.forEach { sessionId ->
                val closed = closeSession(sessionId, now, attempt)
                if (closed == null) failed += sessionId else closedCount += closed
            }
            pending = failed
        }

        if (pending.isNotEmpty()) {
            logger.error { "Auto absence gave up after $MAX_ATTEMPTS attempts: sessionIds=${pending.map { it.value }}" }
        }
        return closedCount
    }

    /** 실패하면 null 을 돌려준다. */
    private fun closeSession(
        sessionId: SessionId,
        now: Instant,
        attempt: Int,
    ): Int? =
        try {
            attendanceCommandService.closeExpiredAttendances(sessionId = sessionId, now = now).also { closed ->
                if (closed > 0) {
                    logger.info { "Auto absence: sessionId=$sessionId, closed=$closed" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Auto absence failed for sessionId=$sessionId (attempt $attempt/$MAX_ATTEMPTS)" }
            null
        }

    // 테스트에서 실제로 기다리지 않도록 재정의한다.
    internal fun waitBeforeRetry(delay: Duration) = Thread.sleep(delay.toMillis())

    companion object {
        const val MAX_ATTEMPTS = 3
        val RETRY_DELAY: Duration = Duration.ofSeconds(5)
    }
}
