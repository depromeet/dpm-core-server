package core.application.attendance.application.service

import core.domain.session.port.outbound.SessionPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 마감이 지난 모든 기수(과거 포함, 하한 없음) 세션의 미인증 출석을 자동 결석으로 바꾼다.
 * 세션마다 별도 트랜잭션으로 처리해 한 세션의 실패가 다른 세션을 막지 않는다. 조건부 UPDATE 라 반복 실행해도 같다.
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
