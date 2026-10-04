package core.application.attendance.application.service

import core.application.support.AttendanceTestFixture
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/** 대상 선정과 갱신 조건은 MySQL 통합 테스트에서 검증하고, 여기서는 세션별 실패 격리와 재시도만 본다. */
class AttendanceAutoAbsenceServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()

    private val calls = mutableMapOf<SessionId, Int>()
    private val waits = mutableListOf<Duration>()

    @Test
    fun `한 세션 처리 실패가 다른 세션 처리를 막지 않는다`() {
        val first = createSession(offsetSeconds = 3600)
        val second = createSession(offsetSeconds = 7200)
        fixture.addAttendance(first, memberId = 1L)
        val secondId = fixture.addAttendance(second, memberId = 1L)
        fixture.clock.now = second.attendancePolicy.absentStart

        val closed = serviceFailing { sessionId, _ -> sessionId == first.id }.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(secondId).status).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `일시적으로 실패한 세션은 재시도로 처리하고 성공한 세션은 다시 처리하지 않는다`() {
        val flaky = createSession(offsetSeconds = 3600)
        val healthy = createSession(offsetSeconds = 7200)
        val flakyId = fixture.addAttendance(flaky, memberId = 1L)
        val healthyId = fixture.addAttendance(healthy, memberId = 1L)
        fixture.clock.now = healthy.attendancePolicy.absentStart

        val closed = serviceFailing { sessionId, attempt -> sessionId == flaky.id && attempt == 1 }.closeExpiredAttendances()

        assertThat(closed).isEqualTo(2)
        assertThat(fixture.attendances.row(flakyId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(healthyId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(calls).containsEntry(flaky.id, 2).containsEntry(healthy.id, 1)
        assertThat(waits).containsExactly(AttendanceAutoAbsenceService.RETRY_DELAY)
    }

    @Test
    fun `계속 실패하는 세션은 최대 시도 횟수까지만 재시도하고 나머지 세션은 처리한다`() {
        val broken = createSession(offsetSeconds = 3600)
        val healthy = createSession(offsetSeconds = 7200)
        val brokenId = fixture.addAttendance(broken, memberId = 1L)
        val healthyId = fixture.addAttendance(healthy, memberId = 1L)
        fixture.clock.now = healthy.attendancePolicy.absentStart

        val closed = serviceFailing { sessionId, _ -> sessionId == broken.id }.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(brokenId).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(healthyId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(calls)
            .containsEntry(broken.id, AttendanceAutoAbsenceService.MAX_ATTEMPTS)
            .containsEntry(healthy.id, 1)
        assertThat(waits).hasSize(AttendanceAutoAbsenceService.MAX_ATTEMPTS - 1)
    }

    @Test
    fun `실패가 없으면 기다리지 않는다`() {
        val session = createSession(offsetSeconds = 3600)
        fixture.addAttendance(session, memberId = 1L)
        fixture.clock.now = session.attendancePolicy.absentStart

        val closed = serviceFailing { _, _ -> false }.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(waits).isEmpty()
    }

    private fun createSession(offsetSeconds: Long): Session = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(offsetSeconds)))

    /** [shouldFail]에 세션과 그 세션의 시도 차수(1부터)를 넘겨 실패를 주입하고, 재시도 대기는 기록만 한다. */
    private fun serviceFailing(shouldFail: (SessionId, Int) -> Boolean): AttendanceAutoAbsenceService =
        object : AttendanceAutoAbsenceService(
            sessionPersistencePort = fixture.sessions,
            attendanceCommandService =
                object : AttendanceCommandServiceDelegate(fixture) {
                    override fun closeExpiredAttendances(
                        sessionId: SessionId,
                        now: Instant,
                    ): Int {
                        val attempt = calls.merge(sessionId, 1, Int::plus)!!
                        if (shouldFail(sessionId, attempt)) throw IllegalStateException("boom")
                        return super.closeExpiredAttendances(sessionId, now)
                    }
                },
            clock = fixture.clock,
        ) {
            override fun waitBeforeRetry(delay: Duration) {
                waits += delay
            }
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
