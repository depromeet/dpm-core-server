package core.application.session.presentation.mapper

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class SessionMapperTest {
    private val sessionStart = Instant.parse("2026-09-05T05:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)
    private val session =
        Session(
            id = SessionId(1L),
            cohortId = CohortId(18L),
            date = sessionStart,
            week = 1,
            place = "온라인",
            eventName = "1주차 세션",
            attendancePolicy =
                AttendancePolicy(
                    attendanceStart = times.attendanceStart,
                    lateStart = times.lateStart,
                    absentStart = times.absentStart,
                    attendanceCode = "1234",
                ),
        )
    private val attendedAt = Instant.parse("2026-09-05T04:58:21Z")

    @Test
    fun `디퍼 세션 상세는 운영진이 바꾼 예전 기록(인증 시각이 남음)의 인증 시각을 숨기고 바꾸지 않은 기록은 그대로 준다`() {
        val manual = attendance(updatedAt = Instant.parse("2026-09-05T06:00:00Z"))
        val untouched = attendance(updatedAt = null)

        assertThat(SessionMapper.toSessionDetailForDeeperResponse(session, manual).attendedAt).isNull()
        assertThat(SessionMapper.toSessionDetailForDeeperResponse(session, untouched).attendedAt)
            .isEqualTo(LocalDateTime.parse("2026-09-05T13:58:21"))
    }

    private fun attendance(updatedAt: Instant?) =
        Attendance(
            sessionId = SessionId(1L),
            memberId = MemberId(1L),
            status = AttendanceStatus.PRESENT,
            attendedAt = attendedAt,
            updatedAt = updatedAt,
        )
}
