package core.application.session.application.service

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.session.application.exception.InvalidAttendanceTimeOrderException
import core.application.session.application.exception.PartialAttendanceTimesException
import core.application.support.AttendanceTestFixture
import core.domain.session.aggregate.Session
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SessionCommandServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")

    @Test
    fun `출석 시각을 모두 생략하면 설정한 기본값으로 계산한다`() {
        val custom = AttendanceTestFixture(now = now, policyProperties = AttendancePolicyProperties(20, 5, 45))
        custom.createActiveCohort()

        custom.sessionCommandService.createSession(createCommand(sessionStart))

        val created = custom.sessions.all().single()
        assertThat(created.attendancePolicy.attendanceStart).isEqualTo(sessionStart.minus(Duration.ofMinutes(20)))
        assertThat(created.attendancePolicy.lateStart).isEqualTo(sessionStart.plus(Duration.ofMinutes(5)))
        assertThat(created.attendancePolicy.absentStart).isEqualTo(sessionStart.plus(Duration.ofMinutes(45)))
    }

    @Test
    fun `출석 시각을 모두 주면 기본값과 다르더라도 그대로 저장한다`() {
        val attendanceStart = sessionStart
        val lateStart = sessionStart.plus(Duration.ofMinutes(20))
        val absentStart = sessionStart.plus(Duration.ofMinutes(35))

        fixture.sessionCommandService.createSession(createCommand(sessionStart, attendanceStart, lateStart, absentStart))

        val created = onlySession()
        assertThat(created.attendancePolicy.attendanceStart).isEqualTo(attendanceStart)
        assertThat(created.attendancePolicy.lateStart).isEqualTo(lateStart)
        assertThat(created.attendancePolicy.absentStart).isEqualTo(absentStart)
    }

    @Test
    fun `출석 시각을 일부만 주거나 순서가 틀리면 400 이고 아무것도 만들지 않는다`() {
        val partials =
            listOf(
                createCommand(sessionStart, attendanceStart = sessionStart),
                createCommand(sessionStart, lateStart = sessionStart),
                createCommand(sessionStart, absentStart = sessionStart),
                createCommand(sessionStart, attendanceStart = sessionStart, lateStart = sessionStart.plusSeconds(60)),
            )
        val misordered =
            listOf(
                createCommand(sessionStart, sessionStart, sessionStart.plusSeconds(60), sessionStart.plusSeconds(60)),
                createCommand(sessionStart, sessionStart.plusSeconds(120), sessionStart.plusSeconds(60), sessionStart.plusSeconds(180)),
            )

        partials.forEach { command ->
            assertThatThrownBy { fixture.sessionCommandService.createSession(command) }
                .isInstanceOf(PartialAttendanceTimesException::class.java)
        }
        misordered.forEach { command ->
            assertThatThrownBy { fixture.sessionCommandService.createSession(command) }
                .isInstanceOf(InvalidAttendanceTimeOrderException::class.java)
        }
        assertThat(fixture.sessions.all()).isEmpty()
        assertThat(fixture.events).isEmpty()
    }

    @Test
    fun `기본값 설정을 바꿔 재시작해도 기존 세션은 그대로이고 이후 생성 세션에만 적용된다`() {
        fixture.sessionCommandService.createSession(createCommand(sessionStart))
        val before = onlySession()

        val restarted = restartedWith(AttendancePolicyProperties(5, 25, 40))

        val unchanged = fixture.sessions.stored(before.id!!.value)
        assertThat(unchanged.attendancePolicy).usingRecursiveComparison().isEqualTo(before.attendancePolicy)

        val nextStart = sessionStart.plus(Duration.ofDays(7))
        restarted.createSession(createCommand(nextStart))
        val after = fixture.sessions.all().first { it.id != before.id }
        assertThat(after.attendancePolicy.attendanceStart).isEqualTo(nextStart.minus(Duration.ofMinutes(5)))
        assertThat(after.attendancePolicy.lateStart).isEqualTo(nextStart.plus(Duration.ofMinutes(25)))
        assertThat(after.attendancePolicy.absentStart).isEqualTo(nextStart.plus(Duration.ofMinutes(40)))
    }

    @Test
    fun `인증 시작만 바꿀 때 날짜가 달라도 순서만 맞으면 허용한다`() {
        val kst = ZoneId.of("Asia/Seoul")
        val midnight = LocalDateTime.of(2026, 10, 11, 0, 5).atZone(kst).toInstant()
        fixture.sessionCommandService.createSession(createCommand(midnight))
        val session = onlySession()
        val previousDay = LocalDateTime.of(2026, 10, 10, 23, 40).atZone(kst).toInstant()

        fixture.sessionCommandService.updateSessionStartTime(session.id!!, previousDay)

        assertThat(fixture.sessions.stored(session.id!!.value).attendancePolicy.attendanceStart).isEqualTo(previousDay)
    }

    @Test
    fun `시각 순서가 틀린 수정은 400 이고 저장하지 않는다`() {
        fixture.sessionCommandService.createSession(createCommand(sessionStart))
        val session = onlySession()

        assertThatThrownBy {
            fixture.sessionCommandService.updateSessionStartTime(session.id!!, session.attendancePolicy.lateStart)
        }.isInstanceOf(InvalidAttendanceTimeOrderException::class.java)
        assertThatThrownBy {
            fixture.sessionCommandService.updateSession(
                updateCommand(session, session.attendancePolicy.absentStart, session.attendancePolicy.lateStart),
            )
        }.isInstanceOf(InvalidAttendanceTimeOrderException::class.java)

        assertThat(fixture.sessions.stored(session.id!!.value).attendancePolicy)
            .usingRecursiveComparison()
            .isEqualTo(session.attendancePolicy)
    }

    private fun onlySession(): Session = fixture.sessions.all().single()

    /** 같은 저장소(DB)를 쓰면서 기본값 설정만 바꿔 재시작한 서버를 흉내 낸다. */
    private fun restartedWith(properties: AttendancePolicyProperties): SessionCommandService =
        SessionCommandService(
            sessionPersistencePort = fixture.sessions,
            eventPublisher = ApplicationEventPublisher { fixture.events += it },
            sessionValidator = fixture.sessionValidator,
            cohortQueryService = fixture.cohortQueryService,
            sentSessionNotificationCommandUseCase = fixture.notifications,
            attendancePolicyProperties = properties,
        )

    private fun createCommand(
        date: Instant,
        attendanceStart: Instant? = null,
        lateStart: Instant? = null,
        absentStart: Instant? = null,
    ) = SessionCreateCommand(
        date = date,
        week = 1,
        place = null,
        eventName = null,
        isOnline = null,
        attendanceStart = attendanceStart,
        lateStart = lateStart,
        absentStart = absentStart,
    )

    private fun updateCommand(
        session: Session,
        lateStart: Instant,
        absentStart: Instant,
    ) = SessionUpdateCommand(
        sessionId = session.id!!,
        date = session.date,
        week = session.week,
        place = session.place,
        eventName = session.eventName,
        isOnline = session.isOnline,
        attendanceStart = session.attendancePolicy.attendanceStart,
        lateStart = lateStart,
        absentStart = absentStart,
    )
}
