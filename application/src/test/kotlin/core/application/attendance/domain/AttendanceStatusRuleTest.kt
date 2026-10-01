package core.application.attendance.domain

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AttendanceStatusRuleTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val lateStart = sessionStart.plusSeconds(900)
    private val absentStart = sessionStart.plusSeconds(1800)
    private val beforeClose = absentStart.minusSeconds(60)
    private val afterClose = absentStart.plusSeconds(60)
    private val decidedAt = sessionStart.minusSeconds(3600)

    @Test
    fun `미인증만 인증으로 기록할 수 있다`() {
        val notRecordable =
            listOf(
                attendance(AttendanceStatus.PRESENT, attendedAt = sessionStart),
                attendance(AttendanceStatus.LATE, attendedAt = lateStart),
                attendance(AttendanceStatus.ABSENT),
                attendance(AttendanceStatus.EXCUSED_ABSENT),
                attendance(AttendanceStatus.EARLY_LEAVE),
                attendance(AttendanceStatus.PENDING, updatedAt = decidedAt),
                attendance(AttendanceStatus.ABSENT, updatedAt = decidedAt),
                attendance(AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt),
            )

        assertThat(attendance(AttendanceStatus.PENDING).canRecordAttendance()).isTrue()
        notRecordable.forEach { assertThat(it.canRecordAttendance()).describedAs(it.describe()).isFalse() }
    }

    @Test
    fun `인증 기록은 새 지각 시작과 마감으로 다시 판정한다`() {
        val attendedAt = sessionStart.plusSeconds(1200) // T+20

        assertThat(
            attendance(AttendanceStatus.LATE, attendedAt = attendedAt)
                .recalculateStatusByPolicy(sessionStart.plusSeconds(1500), absentStart, afterClose),
        ).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(
            attendance(AttendanceStatus.PRESENT, attendedAt = sessionStart)
                .recalculateStatusByPolicy(sessionStart, absentStart, afterClose),
        ).isEqualTo(AttendanceStatus.LATE)
        assertThat(
            attendance(AttendanceStatus.LATE, attendedAt = attendedAt)
                .recalculateStatusByPolicy(sessionStart, attendedAt, afterClose),
        ).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `상태가 그대로면 null 이다`() {
        assertThat(
            attendance(AttendanceStatus.LATE, attendedAt = lateStart)
                .recalculateStatusByPolicy(lateStart, absentStart, afterClose),
        ).isNull()
        assertThat(attendance(AttendanceStatus.PENDING).recalculateStatusByPolicy(lateStart, absentStart, afterClose))
            .isNull()
        assertThat(attendance(AttendanceStatus.PENDING).recalculateStatusByPolicy(lateStart, absentStart, beforeClose))
            .isNull()
    }

    @Test
    fun `운영진이 정한 기록은 시각이 바뀌어도 재계산하지 않는다`() {
        val manualLate = attendance(AttendanceStatus.LATE, attendedAt = sessionStart, updatedAt = afterClose)
        val manualAbsent = attendance(AttendanceStatus.ABSENT, updatedAt = sessionStart)

        assertThat(manualLate.recalculateStatusByPolicy(sessionStart.plusSeconds(1), absentStart, afterClose)).isNull()
        assertThat(manualAbsent.recalculateStatusByPolicy(lateStart, afterClose.plusSeconds(600), afterClose)).isNull()
    }

    @Test
    fun `인정 결석과 조퇴는 재계산 대상이 아니다`() {
        assertThat(
            attendance(AttendanceStatus.EXCUSED_ABSENT, attendedAt = sessionStart)
                .recalculateStatusByPolicy(sessionStart.minusSeconds(1), absentStart, afterClose),
        ).isNull()
        assertThat(
            attendance(AttendanceStatus.EARLY_LEAVE, attendedAt = sessionStart)
                .recalculateStatusByPolicy(sessionStart.plusSeconds(1), absentStart, afterClose),
        ).isNull()
    }

    private fun Attendance.describe() = "status=$status attendedAt=$attendedAt updatedAt=$updatedAt"

    private fun attendance(
        status: AttendanceStatus,
        attendedAt: Instant? = null,
        updatedAt: Instant? = null,
    ) = Attendance(
        id = AttendanceId(1L),
        sessionId = SessionId(1L),
        memberId = MemberId(1L),
        status = status,
        attendedAt = attendedAt,
        updatedAt = updatedAt,
    )
}
