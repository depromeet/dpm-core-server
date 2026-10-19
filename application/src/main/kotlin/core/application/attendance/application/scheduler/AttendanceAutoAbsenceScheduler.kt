package core.application.attendance.application.scheduler

import core.application.attendance.application.service.AttendanceAutoAbsenceService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 모든 세션의 출석 인증은 19시(KST) 전에 마감되도록 운영하므로, 매일 19시에 한 번 그날 마감된 세션을 처리한다.
 * 끝내 실패한 세션과 대상 조회가 실패한 실행의 세션은 다음 날 실행에서 함께 처리된다(대상 조회에 시각 하한이 없다).
 * 대상 조회·세션별 실패는 서비스가 직접 로그를 남기므로 여기서는 그 밖의 예기치 못한 예외만 남긴다.
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
            logger.error(e) { "Auto absence scheduler failed unexpectedly" }
        }
    }

    companion object {
        const val AUTO_ABSENCE_CRON = "0 0 19 * * *"
        const val AUTO_ABSENCE_ZONE = "Asia/Seoul"
    }
}
