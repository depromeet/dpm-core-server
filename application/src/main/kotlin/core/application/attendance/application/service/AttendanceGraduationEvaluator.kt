package core.application.attendance.application.service

import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import org.springframework.stereotype.Component

/**
 * 수료 가능 여부를 판정한다. 조회할 때마다 현재 세션/출석 기록으로 계산하며 결과를 저장하지 않는다.
 *
 * 수료 조건: 출석률 80% 이상 달성 가능, 환산 결석 4회 이하, 오프라인 실제 결석 2회 이하.
 *
 * 결석 환산은 버림 없이 0.5 단위로 계산하기 위해 2배 정수(halfUnits)로 다룬다.
 * - 결석(ABSENT) 1회 = 2, 지각(LATE) 1회 = 1(결석 0.5회)
 * - 인정 결석(EXCUSED_ABSENT)은 출석으로 인정해 0
 * - 미인증(PENDING, 아직 열리지 않았거나 마감 전인 세션)과 레거시 조퇴(EARLY_LEAVE)는 0
 *
 * 결석은 줄어들지 않으므로 조건 하나라도 이미 넘었으면 더는 수료할 수 없다(IMPOSSIBLE).
 * 출석률은 앞으로 남은 세션을 모두 출석했을 때의 최대치 (N - 환산 결석) / N 으로 본다.
 * 분모 N 은 대상 기수의 삭제되지 않은 전체 세션 수다.
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
                summary.offlineAbsentCount > MAX_OFFLINE_ABSENT_COUNT ->
                AttendanceGraduationStatus.IMPOSSIBLE
            absenceHalfUnits >= AT_RISK_ABSENCE_HALF_UNITS ||
                summary.offlineAbsentCount >= MAX_OFFLINE_ABSENT_COUNT ->
                AttendanceGraduationStatus.AT_RISK
            else -> AttendanceGraduationStatus.NORMAL
        }
    }

    companion object {
        /** 수료 가능한 최대 환산 결석 4회. 4.5회부터 수료 불가 */
        const val MAX_ABSENCE_HALF_UNITS = 8

        /** 환산 결석 3회 이상이면 수료 위험 */
        const val AT_RISK_ABSENCE_HALF_UNITS = 6

        /** 수료 가능한 최대 오프라인 실제 결석 2회. 2회면 위험, 3회부터 수료 불가 */
        const val MAX_OFFLINE_ABSENT_COUNT = 2
    }
}
