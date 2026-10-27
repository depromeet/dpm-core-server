package core.application.attendance.application.service

import core.domain.attendance.enums.AttendanceGraduationStatus.AT_RISK
import core.domain.attendance.enums.AttendanceGraduationStatus.IMPOSSIBLE
import core.domain.attendance.enums.AttendanceGraduationStatus.NORMAL
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AttendanceGraduationEvaluatorTest {
    private val evaluator = AttendanceGraduationEvaluator()

    @Test
    fun `수료 판정 경계`() {
        val cases =
            listOf(
                // 세션 0개, 모두 미인증(집계 0)
                Case(sessions = 0) to NORMAL,
                Case(sessions = 20) to NORMAL,
                // 지각은 0.5회. 버리지 않는다(5회 = 2.5 → 정상, 6회 = 3.0 → 위험)
                Case(sessions = 40, late = 5) to NORMAL,
                Case(sessions = 40, late = 6) to AT_RISK,
                Case(sessions = 40, online = 2, late = 1) to NORMAL,
                Case(sessions = 40, online = 2, late = 2) to AT_RISK,
                // 인정 결석은 출석으로 본다
                Case(sessions = 40, excused = 10) to NORMAL,
                // 전체 환산 결석은 3.5회까지 위험, 4회부터 불가
                Case(sessions = 40, online = 3, late = 1) to AT_RISK,
                Case(sessions = 40, online = 4) to IMPOSSIBLE,
                Case(sessions = 40, late = 8) to IMPOSSIBLE,
                Case(sessions = 40, online = 4, late = 1) to IMPOSSIBLE,
                Case(sessions = 40, late = 9) to IMPOSSIBLE,
                // 오프라인 환산 결석 2.5회는 위험, 3회부터 불가
                Case(sessions = 40, offline = 1, online = 1) to NORMAL,
                Case(sessions = 40, offline = 2) to AT_RISK,
                Case(sessions = 40, offline = 3) to IMPOSSIBLE,
                Case(sessions = 40, offline = 2, late = 1, offlineLate = 1) to AT_RISK,
                Case(sessions = 40, offline = 2, late = 2, offlineLate = 2) to IMPOSSIBLE,
                Case(sessions = 40, late = 5, offlineLate = 5) to AT_RISK,
                Case(sessions = 40, late = 6, offlineLate = 6) to IMPOSSIBLE,
                // 온라인 지각은 오프라인 환산 결석에 더하지 않는다
                Case(sessions = 40, offline = 2, late = 2) to AT_RISK,
                // 출석률은 판정하지 않는다. 남은 세션 수도 판정을 바꾸지 않는다
                Case(sessions = 20, online = 4) to IMPOSSIBLE,
                Case(sessions = 5, online = 1) to NORMAL,
                Case(sessions = 5, online = 1, late = 1) to NORMAL,
                Case(sessions = 2, online = 2) to NORMAL,
                Case(sessions = 40, online = 2) to NORMAL,
            )

        cases.forEach { (case, expected) ->
            assertThat(evaluator.evaluate(case.toSummary())).describedAs("$case").isEqualTo(expected)
        }
    }

    private data class Case(
        val sessions: Int,
        val late: Int = 0,
        val offlineLate: Int = 0,
        val excused: Int = 0,
        val online: Int = 0,
        val offline: Int = 0,
    ) {
        fun toSummary() =
            AttendanceSummaryQueryModel(
                totalSessionCount = sessions,
                presentCount = 0,
                lateCount = late,
                excusedAbsentCount = excused,
                onlineAbsentCount = online,
                offlineAbsentCount = offline,
                offlineLateCount = offlineLate,
            )
    }
}
