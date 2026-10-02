package core.application.attendance.application.service

import core.application.support.AttendanceTestFixture
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/** 대상 선정과 갱신 조건은 MySQL 통합 테스트에서 검증하고, 여기서는 세션별 실패 격리만 본다. */
class AttendanceAutoAbsenceServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()

    @Test
    fun `한 세션 처리 실패가 다른 세션 처리를 막지 않는다`() {
        val first = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(3600)))
        val second = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(7200)))
        fixture.addAttendance(first, memberId = 1L)
        val secondId = fixture.addAttendance(second, memberId = 1L)

        val failing =
            AttendanceAutoAbsenceService(
                sessionPersistencePort = fixture.sessions,
                attendanceCommandService =
                    object : AttendanceCommandServiceDelegate(fixture) {
                        override fun closeExpiredAttendances(
                            sessionId: SessionId,
                            now: Instant,
                        ): Int {
                            if (sessionId == first.id) throw IllegalStateException("boom")
                            return super.closeExpiredAttendances(sessionId, now)
                        }
                    },
                clock = fixture.clock,
            )
        fixture.clock.now = second.attendancePolicy.absentStart

        val closed = failing.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(secondId).status).isEqualTo(AttendanceStatus.ABSENT)
    }

    // kotlin-spring 으로 open 된 실제 서비스의 하위 클래스로 실패를 주입한다.
    private open class AttendanceCommandServiceDelegate(
        fixture: AttendanceTestFixture,
    ) : AttendanceCommandService(
            attendancePersistencePort = fixture.attendances,
            sessionPersistencePort = fixture.sessions,
            memberQueryUseCase = fixture.memberQueryUseCase,
            sessionValidator = fixture.sessionValidator,
            clock = fixture.clock,
        )
}
