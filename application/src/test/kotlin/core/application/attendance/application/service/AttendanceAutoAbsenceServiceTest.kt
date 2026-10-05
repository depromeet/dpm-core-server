package core.application.attendance.application.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import core.application.support.AttendanceTestFixture
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/** 실제 쿼리와 잠금은 MySQL 통합 테스트에서 검증하고, 여기서는 활성 기수 선택, PENDING 기준 대상, 세션별 실패 격리와 재시도, 실패 로그를 본다. */
class AttendanceAutoAbsenceServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()

    private val calls = mutableMapOf<SessionId, Int>()
    private val waits = mutableListOf<Duration>()

    private val serviceLogger = LoggerFactory.getLogger(AttendanceAutoAbsenceService::class.java) as Logger
    private val logs = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun attachLogs() {
        logs.start()
        serviceLogger.addAppender(logs)
    }

    @AfterEach
    fun detachLogs() {
        serviceLogger.detachAppender(logs)
    }

    @Test
    fun `활성 기수 세션만 처리하고 비활성 기수 세션은 그대로 둔다`() {
        val inactiveCohortId = fixture.cohorts.save(Cohort(value = "17")).id!!
        val active = createSession(offsetSeconds = 3600)
        val inactive = createSession(offsetSeconds = 3600, cohort = inactiveCohortId)
        val activeId = fixture.addAttendance(active, memberId = 1L)
        val inactiveId = fixture.addAttendance(inactive, memberId = 1L)
        fixture.clock.now = active.attendancePolicy.absentStart

        val closed = serviceFailing { _, _ -> false }.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(activeId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(inactiveId).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(calls.keys).containsExactly(active.id)
    }

    @Test
    fun `활성 기수가 없으면 최근 기수로 대신하지 않고 아무것도 처리하지 않는다`() {
        val session = createSession(offsetSeconds = 3600)
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.clock.now = session.attendancePolicy.absentStart
        fixture.cohorts.deactivateAll()

        val closed = serviceFailing { _, _ -> false }.closeExpiredAttendances()

        assertThat(closed).isZero()
        assertThat(calls).isEmpty()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(logs.list.filter { it.level.isGreaterOrEqual(Level.WARN) }).isEmpty()
    }

    @Test
    fun `현재 PENDING 이면 인증·운영진 시각이 남아 있어도 결석이 되고 그 밖의 기록은 바뀌지 않으며 반복해도 같다`() {
        val session = createSession(offsetSeconds = 3600)
        val decidedAt = now.minusSeconds(60)
        val attendedAt = session.attendancePolicy.lateStart
        val plain = fixture.addAttendance(session, memberId = 1L)
        val adminReset = fixture.addAttendance(session, memberId = 2L, updatedAt = decidedAt)
        val adminResetAfterAttend =
            fixture.addAttendance(session, memberId = 3L, attendedAt = attendedAt, updatedAt = decidedAt)
        val adminPresent =
            fixture.addAttendance(session, memberId = 4L, status = AttendanceStatus.PRESENT, updatedAt = decidedAt)
        val adminExcused =
            fixture.addAttendance(session, memberId = 5L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val late = fixture.addAttendance(session, memberId = 6L, status = AttendanceStatus.LATE, attendedAt = attendedAt)
        val legacyAbsent = fixture.addAttendance(session, memberId = 7L, status = AttendanceStatus.ABSENT)
        val deleted = fixture.attendances.insert(session.id!!.value, memberId = 8L, deletedAt = decidedAt)
        fixture.clock.now = session.attendancePolicy.absentStart

        val first = serviceFailing { _, _ -> false }.closeExpiredAttendances()
        val second = serviceFailing { _, _ -> false }.closeExpiredAttendances()

        assertThat(first).isEqualTo(3)
        assertThat(second).isZero()
        listOf(plain, adminReset, adminResetAfterAttend).forEach {
            val row = fixture.attendances.row(it)
            assertThat(row.status).isEqualTo(AttendanceStatus.ABSENT)
            assertThat(row.autoAbsentAt).isEqualTo(session.attendancePolicy.absentStart)
        }
        assertThat(fixture.attendances.row(adminReset).updatedAt).isEqualTo(decidedAt)
        assertThat(fixture.attendances.row(adminResetAfterAttend).attendedAt).isEqualTo(attendedAt)
        assertThat(fixture.attendances.row(adminResetAfterAttend).updatedAt).isEqualTo(decidedAt)
        assertThat(fixture.attendances.row(adminPresent).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(adminExcused).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(late).status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(legacyAbsent).autoAbsentAt).isNull()
        assertThat(fixture.attendances.row(deleted).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(deleted).autoAbsentAt).isNull()
    }

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

        // 시도별 실패는 스택과 함께 WARN, 끝내 실패한 세션은 실행당 ERROR 요약 한 번(스택 없이)
        val warns = logs.list.filter { it.level == Level.WARN }
        assertThat(warns.map { it.formattedMessage }).containsExactly(
            "Auto absence failed: sessionId=${broken.id!!.value}, attempt=1/3, will retry",
            "Auto absence failed: sessionId=${broken.id!!.value}, attempt=2/3, will retry",
            "Auto absence failed: sessionId=${broken.id!!.value}, attempt=3/3, no attempts left",
        )
        assertThat(warns).allSatisfy { assertThat(it.throwableProxy.message).isEqualTo("boom") }
        val errors = logs.list.filter { it.level == Level.ERROR }
        assertThat(errors).singleElement().satisfies({
            assertThat(it.formattedMessage).isEqualTo(
                "Auto absence gave up after 3 attempts: failedSessionIds=[${broken.id!!.value}], succeeded=1, failed=1, total=2",
            )
            assertThat(it.throwableProxy).isNull()
        })
    }

    @Test
    fun `일시적 실패가 재시도로 회복되면 ERROR 를 남기지 않는다`() {
        val flaky = createSession(offsetSeconds = 3600)
        fixture.addAttendance(flaky, memberId = 1L)
        fixture.clock.now = flaky.attendancePolicy.absentStart

        serviceFailing { _, attempt -> attempt == 1 }.closeExpiredAttendances()

        assertThat(logs.list.filter { it.level == Level.WARN }).hasSize(1)
        assertThat(logs.list.filter { it.level == Level.ERROR }).isEmpty()
    }

    @Test
    fun `대상 조회가 실패하면 세션별 실패와 구분되는 ERROR 를 한 번 남기고 예외를 밖으로 던지지 않는다`() {
        val failingLookup =
            object : SessionPersistencePort by fixture.sessions {
                override fun findSessionIdsToAutoClose(
                    cohortId: CohortId,
                    absentStartTo: Instant,
                ): List<SessionId> = throw IllegalStateException("db down")
            }

        val closed = serviceFailing(sessions = failingLookup) { _, _ -> false }.closeExpiredAttendances()

        assertThat(closed).isZero()
        assertThat(calls).isEmpty()
        assertThat(logs.list).singleElement().satisfies({
            assertThat(it.level).isEqualTo(Level.ERROR)
            assertThat(it.formattedMessage).startsWith("Auto absence candidate lookup failed")
            assertThat(it.throwableProxy.message).isEqualTo("db down")
        })
    }

    @Test
    fun `재시도 대기 중 인터럽트되면 인터럽트 상태를 유지하고 남은 재시도를 멈춘다`() {
        val broken = createSession(offsetSeconds = 3600)
        fixture.addAttendance(broken, memberId = 1L)
        fixture.clock.now = broken.attendancePolicy.absentStart
        val service = serviceFailing(recordWaits = false) { _, _ -> true }

        Thread.currentThread().interrupt()
        try {
            service.closeExpiredAttendances()

            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
        assertThat(calls).containsEntry(broken.id, 1)
        assertThat(logs.list.filter { it.level == Level.ERROR }.map { it.formattedMessage }).containsExactly(
            "Auto absence interrupted while waiting to retry: failedSessionIds=[${broken.id!!.value}], succeeded=0, failed=1, total=1",
        )
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

    private fun createSession(
        offsetSeconds: Long,
        cohort: CohortId = cohortId,
    ): Session = fixture.createSession(cohort, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(offsetSeconds)))

    /**
     * [shouldFail]에 세션과 그 세션의 시도 차수(1부터)를 넘겨 실패를 주입한다.
     * [recordWaits]면 재시도 대기는 기록만 하고, 아니면 실제 대기 로직을 쓴다.
     */
    private fun serviceFailing(
        sessions: SessionPersistencePort = fixture.sessions,
        recordWaits: Boolean = true,
        shouldFail: (SessionId, Int) -> Boolean,
    ): AttendanceAutoAbsenceService =
        object : AttendanceAutoAbsenceService(
            cohortPersistencePort = fixture.cohorts,
            sessionPersistencePort = sessions,
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
            override fun waitBeforeRetry(delay: Duration): Boolean {
                if (!recordWaits) return super.waitBeforeRetry(delay)
                waits += delay
                return true
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
