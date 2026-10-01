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

    @Test
    fun `미인증과 표지가 있는 자동 결석만 인증으로 기록할 수 있다`() {
        assertThat(attendance(AttendanceStatus.PENDING).canRecordAttendance()).isTrue()
        assertThat(attendance(AttendanceStatus.ABSENT, autoAbsentAt = absentStart).canRecordAttendance()).isTrue()

        assertThat(attendance(AttendanceStatus.PRESENT, attendedAt = sessionStart).canRecordAttendance()).isFalse()
        assertThat(attendance(AttendanceStatus.LATE, attendedAt = lateStart).canRecordAttendance()).isFalse()
    }

    @Test
    fun `표지가 없는 기존 결석은 기록이 비어 있어도 자동 결석으로 추정하지 않는다`() {
        val legacyAbsent = attendance(AttendanceStatus.ABSENT)

        assertThat(legacyAbsent.isAutomaticallyAbsent()).isFalse()
        assertThat(legacyAbsent.canRecordAttendance()).isFalse()
        assertThat(legacyAbsent.recalculateStatusByPolicy(lateStart, afterClose.plusSeconds(600), afterClose)).isNull()
    }

    @Test
    fun `자동 결석 표지는 운영진 변경이 있으면 무시된다`() {
        val decided = attendance(AttendanceStatus.ABSENT, updatedAt = sessionStart, autoAbsentAt = absentStart)

        assertThat(decided.isAutomaticallyAbsent()).isFalse()
        assertThat(decided.canRecordAttendance()).isFalse()
    }

    @Test
    fun `자동 결석은 표지를 기록하고 인증과 운영진 변경은 표지를 해제한다`() {
        val autoAbsent = attendance(AttendanceStatus.PENDING)
        autoAbsent.markAutoAbsent(absentStart)
        assertThat(autoAbsent.status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(autoAbsent.autoAbsentAt).isEqualTo(absentStart)
        assertThat(autoAbsent.updatedAt).isNull()
        assertThat(autoAbsent.isAutomaticallyAbsent()).isTrue()

        autoAbsent.markAttendance(AttendanceStatus.LATE, beforeClose)
        assertThat(autoAbsent.autoAbsentAt).isNull()

        val admin = attendance(AttendanceStatus.ABSENT, autoAbsentAt = absentStart)
        admin.updateStatus(AttendanceStatus.ABSENT, afterClose)
        assertThat(admin.autoAbsentAt).isNull()
        assertThat(admin.isAutomaticallyAbsent()).isFalse()
    }

    @Test
    fun `운영진이 정한 기록은 attendedAt 이 없어도 인증으로 덮어쓸 수 없다`() {
        val decidedAt = sessionStart.minusSeconds(3600)

        assertThat(attendance(AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt).canRecordAttendance()).isFalse()
        assertThat(attendance(AttendanceStatus.ABSENT, updatedAt = decidedAt).canRecordAttendance()).isFalse()
        assertThat(attendance(AttendanceStatus.PENDING, updatedAt = decidedAt).canRecordAttendance()).isFalse()
        assertThat(
            attendance(AttendanceStatus.ABSENT, updatedAt = decidedAt, autoAbsentAt = decidedAt).isAutomaticallyAbsent(),
        ).isFalse()
    }

    @Test
    fun `인정 결석은 updatedAt 이 없어도 인증 대상이 아니다`() {
        assertThat(attendance(AttendanceStatus.EXCUSED_ABSENT).canRecordAttendance()).isFalse()
        assertThat(attendance(AttendanceStatus.EARLY_LEAVE).canRecordAttendance()).isFalse()
    }

    @Test
    fun `자동 결석 대상은 운영진 변경과 인증 기록이 없는 미인증뿐이다`() {
        assertThat(attendance(AttendanceStatus.PENDING).isAutoAbsenceTarget()).isTrue()
        assertThat(attendance(AttendanceStatus.PENDING, updatedAt = sessionStart).isAutoAbsenceTarget()).isFalse()
        assertThat(attendance(AttendanceStatus.PENDING, attendedAt = sessionStart).isAutoAbsenceTarget()).isFalse()
        assertThat(attendance(AttendanceStatus.ABSENT).isAutoAbsenceTarget()).isFalse()
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
    fun `운영진이 정한 기록은 재계산하지 않는다`() {
        val manual = attendance(AttendanceStatus.LATE, attendedAt = sessionStart, updatedAt = afterClose)

        assertThat(manual.recalculateStatusByPolicy(sessionStart.plusSeconds(1), absentStart, afterClose)).isNull()
    }

    @Test
    fun `자동 결석은 마감이 연장되어 아직 새 마감 전이면 미인증으로 되돌린다`() {
        val autoAbsent = attendance(AttendanceStatus.ABSENT, autoAbsentAt = absentStart)
        val extendedClose = afterClose.plusSeconds(600)

        assertThat(autoAbsent.recalculateStatusByPolicy(lateStart, extendedClose, extendedClose.minusNanos(1)))
            .isEqualTo(AttendanceStatus.PENDING)
        assertThat(autoAbsent.recalculateStatusByPolicy(lateStart, extendedClose, extendedClose)).isNull()
    }

    @Test
    fun `수동 결석은 마감이 연장돼도 그대로다`() {
        val manualAbsent = attendance(AttendanceStatus.ABSENT, updatedAt = sessionStart)

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

    @Test
    fun `정책 반영은 운영진 변경 표지를 남기지 않고 자동 결석 표지를 해제한다`() {
        val autoAbsent = attendance(AttendanceStatus.ABSENT, autoAbsentAt = absentStart)

        autoAbsent.applyPolicyStatus(AttendanceStatus.PENDING)

        assertThat(autoAbsent.status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(autoAbsent.updatedAt).isNull()
        assertThat(autoAbsent.autoAbsentAt).isNull()
        assertThat(autoAbsent.isAlreadyUpdated()).isFalse()
    }

    @Test
    fun `운영진 변경은 표지를 남긴다`() {
        val target = attendance(AttendanceStatus.PENDING)

        target.updateStatus(AttendanceStatus.EXCUSED_ABSENT, afterClose)

        assertThat(target.updatedAt).isEqualTo(afterClose)
        assertThat(target.isAlreadyUpdated()).isTrue()
    }

    private fun attendance(
        status: AttendanceStatus,
        attendedAt: Instant? = null,
        updatedAt: Instant? = null,
        autoAbsentAt: Instant? = null,
    ) = Attendance(
        id = AttendanceId(1L),
        sessionId = SessionId(1L),
        memberId = MemberId(1L),
        status = status,
        attendedAt = attendedAt,
        updatedAt = updatedAt,
        autoAbsentAt = autoAbsentAt,
    )
}
