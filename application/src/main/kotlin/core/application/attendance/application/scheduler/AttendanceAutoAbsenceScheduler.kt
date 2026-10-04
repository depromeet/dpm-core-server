package core.application.attendance.application.scheduler

import core.application.attendance.application.service.AttendanceAutoAbsenceService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 세션 출석은 대부분 19시 전에 마감되므로 매일 19시(KST)에 한 번 처리한다.
 * 그 뒤에 마감되는 세션과 끝내 실패한 세션은 다음 날 실행에서 함께 처리된다(대상 조회에 시각 하한이 없다).
 */
@Component
class AttendanceAutoAbsenceScheduler(
    private val attendanceAutoAbsenceService: AttendanceAutoAbsenceService,
) {
    private val logger = KotlinLogging.logger { AttendanceAutoAbsenceScheduler::class.java }

    @Scheduled(cron = AUTO_ABSENCE_CRON, zone = AUTO_ABSENCE_ZONE)
    fun closeExpiredAttendances() {
        try {
            attendanceAutoAbsenceService.closeExpiredAttendances()
        } catch (e: Exception) {
            logger.error(e) { "Auto absence scheduler failed" }
        }
    }

    companion object {
        const val AUTO_ABSENCE_CRON = "0 0 19 * * *"
        const val AUTO_ABSENCE_ZONE = "Asia/Seoul"
    }
}
