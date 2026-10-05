package core.application.attendance.application.service

import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import org.springframework.stereotype.Component

/**
 * 수료 판정. 조회마다 계산하며 저장하지 않는다.
 * 조건: 남은 세션을 모두 출석했을 때의 출석률 (N - 환산 결석) / N 이 80% 이상, 환산 결석 4회 이하, 오프라인 실제 결석 2회 이하.
 * 결석은 줄지 않으므로 하나라도 넘으면 IMPOSSIBLE 이다. N 은 대상 기수의 삭제되지 않은 전체 세션 수다.
 * 0.5회 단위를 버림 없이 다루려고 2배 정수(halfUnits)로 센다: ABSENT 2, LATE 1, 인정 결석/미인증/레거시 조퇴 0.
 */
@Component
class AttendanceGraduationEvaluator {
    fun evaluate(summary: AttendanceSummaryQueryModel): AttendanceGraduationStatus {
        val absenceHalfUnits = summary.absentCount * 2 + summary.lateCount
        val totalSessionCount = summary.totalSessionCount
        // (N - halfUnits / 2) / N < 4 / 5  ⇔  5 * halfUnits > 2 * N. 세션이 없으면 출석률 조건은 보지 않는다.
        val cannotReachMinimumRate = totalSessionCount > 0 && 5 * absenceHalfUnits > 2 * totalSessionCount

        return when {
            cannotReachMinimumRate ||
                absenceHalfUnits > MAX_ABSENCE_HALF_UNITS ||
                summary.offlineAbsentCount >= IMPOSSIBLE_OFFLINE_ABSENT_COUNT ->
                AttendanceGraduationStatus.IMPOSSIBLE
            absenceHalfUnits >= AT_RISK_ABSENCE_HALF_UNITS ||
                summary.offlineAbsentCount >= AT_RISK_OFFLINE_ABSENT_COUNT ->
                AttendanceGraduationStatus.AT_RISK
            else -> AttendanceGraduationStatus.NORMAL
        }
    }

    companion object {
        /** 환산 결석 4회까지 수료 가능(4.5회부터 불가) */
        const val MAX_ABSENCE_HALF_UNITS = 8

        /** 환산 결석 3회 이상이면 수료 위험 */
        const val AT_RISK_ABSENCE_HALF_UNITS = 6

        /** 오프라인 실제 결석 3회 이상이면 수료 불가 */
        const val IMPOSSIBLE_OFFLINE_ABSENT_COUNT = 3

        /** 오프라인 실제 결석 2회 이상이면 수료 위험 */
        const val AT_RISK_OFFLINE_ABSENT_COUNT = 2
    }
}
