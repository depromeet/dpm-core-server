package core.application.attendance.application.service

import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 활성 기수에서 마감이 지난(하한 없음) 세션의 미인증(PENDING) 출석을 자동 결석으로 바꾼다.
 * 지난 기수는 처리하지 않고, 활성 기수가 없으면 아무것도 하지 않는다(가장 최근 기수로 대신하지 않는다).
 * 세션마다 별도 트랜잭션으로 처리해 한 세션의 실패가 다른 세션을 막지 않는다. 조건부 UPDATE 라 반복 실행해도 같다.
 * 실패한 세션만 짧게 쉬었다가 최대 [MAX_ATTEMPTS]번까지 다시 시도한다. 활성 기수·대상 조회 자체가 실패하면 다시 시도하지 않고 다음 실행을 기다린다.
 *
 * 시도별 실패는 스택과 함께 WARN 으로, 끝내 처리하지 못한 세션은 실행마다 한 번 ERROR 요약으로 남긴다(ERROR 는 디스코드 알림 대상).
 */
@Service
class AttendanceAutoAbsenceService(
    private val cohortPersistencePort: CohortPersistencePort,
    private val sessionPersistencePort: SessionPersistencePort,
    private val attendanceCommandService: AttendanceCommandService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { AttendanceAutoAbsenceService::class.java }

    fun closeExpiredAttendances(): Int {
        val now = clock.instant()
        val candidates =
            try {
                val cohortId = cohortPersistencePort.findActive()?.id
                if (cohortId == null) {
                    logger.info { "Auto absence skipped: no active cohort" }
                    return 0
                }
                sessionPersistencePort.findSessionIdsToAutoClose(cohortId = cohortId, absentStartTo = now)
            } catch (e: Exception) {
                logger.error(e) { "Auto absence candidate lookup failed; no session processed, next run will retry" }
                return 0
            }

        var pending = candidates
        var closedCount = 0
        var interrupted = false
        for (attempt in 1..MAX_ATTEMPTS) {
            if (pending.isEmpty()) break
            if (attempt > 1 && !waitBeforeRetry(RETRY_DELAY)) {
                interrupted = true
                break
            }

            val failed = mutableListOf<SessionId>()
            pending.forEach { sessionId ->
                val closed = closeSession(sessionId, now, attempt)
                if (closed == null) failed += sessionId else closedCount += closed
            }
            pending = failed
        }

        if (pending.isNotEmpty()) {
            val reason =
                if (interrupted) "interrupted while waiting to retry" else "gave up after $MAX_ATTEMPTS attempts"
            logger.error {
                "Auto absence $reason: failedSessionIds=${pending.map { it.value }}, " +
                    "succeeded=${candidates.size - pending.size}, failed=${pending.size}, total=${candidates.size}"
            }
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
                    logger.info { "Auto absence: sessionId=${sessionId.value}, closed=$closed" }
                }
            }
        } catch (e: Exception) {
            val next = if (attempt < MAX_ATTEMPTS) "will retry" else "no attempts left"
            logger.warn(e) {
                "Auto absence failed: sessionId=${sessionId.value}, attempt=$attempt/$MAX_ATTEMPTS, $next"
            }
            null
        }

    /** 인터럽트되면 인터럽트 상태를 되살리고 false 를 돌려줘 남은 재시도를 멈춘다. 테스트에서 실제로 기다리지 않도록 재정의한다. */
    internal fun waitBeforeRetry(delay: Duration): Boolean =
        try {
            Thread.sleep(delay.toMillis())
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

    companion object {
        const val MAX_ATTEMPTS = 3
        val RETRY_DELAY: Duration = Duration.ofSeconds(5)
    }
}
