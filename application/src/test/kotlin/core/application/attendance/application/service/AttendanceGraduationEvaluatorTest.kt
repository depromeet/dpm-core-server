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
        // 세션 수가 넉넉한 경우(40)는 횟수 기준만, 적은 경우는 80% 달성 가능 여부를 본다.
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
                // 환산 4회까지 수료 가능(위험), 4.5회부터 불가. 세션이 많아 80% 조건과 무관해도 마찬가지
                Case(sessions = 40, online = 4) to AT_RISK,
                Case(sessions = 40, online = 4, late = 1) to IMPOSSIBLE,
                Case(sessions = 40, late = 9) to IMPOSSIBLE,
                // 오프라인 실제 결석 2회는 위험, 3회는 불가. 지각은 오프라인 결석으로 세지 않는다
                Case(sessions = 40, offline = 1, online = 1) to NORMAL,
                Case(sessions = 40, offline = 2) to AT_RISK,
                Case(sessions = 40, offline = 3) to IMPOSSIBLE,
                // 80%: 남은 세션을 모두 출석했을 때 (N - 환산 결석) / N
                Case(sessions = 20, online = 4) to AT_RISK,
                Case(sessions = 5, online = 1) to NORMAL,
                Case(sessions = 5, online = 1, late = 1) to IMPOSSIBLE,
            )

        cases.forEach { (case, expected) ->
            assertThat(evaluator.evaluate(case.toSummary())).describedAs("$case").isEqualTo(expected)
        }
    }

    private data class Case(
        val sessions: Int,
        val late: Int = 0,
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
            )
    }
}
