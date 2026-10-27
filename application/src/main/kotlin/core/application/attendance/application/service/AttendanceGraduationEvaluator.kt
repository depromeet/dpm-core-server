package core.application.attendance.application.service

import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import org.springframework.stereotype.Component

/**
 * 수료 판정. 조회마다 계산하며 저장하지 않는다.
 * 전체 환산 결석 4회 이상 또는 오프라인 환산 결석 3회 이상이면 IMPOSSIBLE 이다.
 * 0.5회 단위를 버림 없이 다루려고 2배 정수(halfUnits)로 센다: ABSENT 2, LATE 1, 인정 결석/미인증/레거시 조퇴 0.
 */
@Component
class AttendanceGraduationEvaluator {
    fun evaluate(summary: AttendanceSummaryQueryModel): AttendanceGraduationStatus {
        val absenceHalfUnits = summary.absentCount.toLong() * 2 + summary.lateCount
        val offlineAbsenceHalfUnits = summary.offlineAbsentCount.toLong() * 2 + summary.offlineLateCount

        return when {
            absenceHalfUnits >= IMPOSSIBLE_ABSENCE_HALF_UNITS ||
                offlineAbsenceHalfUnits >= IMPOSSIBLE_OFFLINE_ABSENCE_HALF_UNITS ->
                AttendanceGraduationStatus.IMPOSSIBLE
            absenceHalfUnits >= AT_RISK_ABSENCE_HALF_UNITS ||
                offlineAbsenceHalfUnits >= AT_RISK_OFFLINE_ABSENCE_HALF_UNITS ->
                AttendanceGraduationStatus.AT_RISK
            else -> AttendanceGraduationStatus.NORMAL
        }
    }

    companion object {
        /** 전체 환산 결석 4회 이상이면 수료 불가 */
        const val IMPOSSIBLE_ABSENCE_HALF_UNITS = 8

        /** 환산 결석 3회 이상이면 수료 위험 */
        const val AT_RISK_ABSENCE_HALF_UNITS = 6

        /** 오프라인 환산 결석 3회 이상이면 수료 불가 */
        const val IMPOSSIBLE_OFFLINE_ABSENCE_HALF_UNITS = 6

        /** 오프라인 환산 결석 2회 이상이면 수료 위험 */
        const val AT_RISK_OFFLINE_ABSENCE_HALF_UNITS = 4
    }
}
