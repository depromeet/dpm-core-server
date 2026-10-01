package core.application.attendance.application.scheduler

import core.application.attendance.application.service.AttendanceAutoAbsenceService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 30초마다 자동 결석을 실행한다. 이전 실행이 끝난 뒤 30초 후 다시 실행한다(fixedDelay).
 * 정상 동작 중에도 마감 이후 DB 반영까지 최대 한 주기 정도의 지연이 있다.
 */
@Component
class AttendanceAutoAbsenceScheduler(
    private val attendanceAutoAbsenceService: AttendanceAutoAbsenceService,
) {
    private val logger = KotlinLogging.logger { AttendanceAutoAbsenceScheduler::class.java }

    @Scheduled(fixedDelay = INTERVAL_MS, initialDelay = INTERVAL_MS)
    fun closeExpiredAttendances() {
        try {
            attendanceAutoAbsenceService.closeExpiredAttendances()
        } catch (e: Exception) {
            logger.error(e) { "Auto absence scheduler failed" }
        }
    }

    companion object {
        const val INTERVAL_MS = 30_000L
    }
}
