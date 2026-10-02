package core.it.attendance

import core.application.attendance.application.service.AttendanceCommandService
import core.application.session.application.exception.CheckedAttendanceException
import core.application.session.application.service.SessionCommandService
import core.application.support.MutableClock
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 실제 MySQL 에서 잠금/조건부 UPDATE 를 검증한다. 기본 test 태스크에서는 제외되며 스키마를 새로 만든다(로컬 dpm_it* DB 만 허용).
 *
 *   DPM_IT_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/dpm_it' DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD=it \
 *   ./gradlew :application:mysqlIntegrationTest
 */
@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(
    classes = [AttendanceMySqlIntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
class AttendanceConcurrencyMySqlIntegrationTest {
    @Autowired lateinit var clock: MutableClock

    @Autowired lateinit var attendanceCommandService: AttendanceCommandService

    @Autowired lateinit var sessionCommandService: SessionCommandService

    @Autowired lateinit var cohortPort: CohortPersistencePort

    @Autowired lateinit var sessionPort: SessionPersistencePort

    @Autowired lateinit var attendancePort: AttendancePersistencePort

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean lateinit var sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase

    private val decidedAt = Instant.parse("2026-01-01T00:00:00Z")
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)

    @BeforeEach
    fun resetClock() {
        clock.now = Instant.parse("2026-10-01T03:00:00Z")
    }

    @Test
    fun `같은 멤버의 동시 인증은 정확히 하나만 저장된다`() {
        val session = newSession()
        addAttendance(session, memberId = 1L)

        val results =
            runConcurrently(12) { index ->
                attend(session, 1L, times.attendanceStart.plusMillis(index.toLong()))
            }

        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.filter { it.isFailure }.map { it.exceptionOrNull() })
            .allMatch { it is CheckedAttendanceException }
        val saved = attendancePort.findAttendanceBy(session.id!!.value, 1L)!!
        assertThat(saved.status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(saved.attendedAt).isNotNull()
        assertThat(saved.updatedAt).isNull()
    }

    @Test
    fun `운영진 결정과 인증이 경쟁해도 운영진 결정이 남는다`() {
        repeat(10) {
            val session = newSession()
            addAttendance(session, memberId = 1L)

            runConcurrently(2) { index ->
                if (index == 0) {
                    attend(session, 1L, times.attendanceStart)
                } else {
                    attendanceCommandService.updateAttendanceStatus(
                        AttendanceStatusUpdateCommand(session.id!!, MemberId(1L), AttendanceStatus.EXCUSED_ABSENT),
                    )
                }
            }

            val row = attendancePort.findAttendanceBy(session.id!!.value, 1L)!!
            assertThat(row.status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
            assertThat(row.updatedAt).isNotNull()
        }
    }

    @Test
    fun `세션 시각 변경과 인증이 경쟁해도 최종 상태는 최신 시각 기준이고 운영진 표지를 남기지 않는다`() {
        repeat(10) {
            val session = newSession()
            addAttendance(session, memberId = 1L)
            val attendedAt = sessionStart.plus(Duration.ofMinutes(20)) // 기존 LATE, 새 시각 PRESENT
            val newLate = sessionStart.plus(Duration.ofMinutes(25))

            runConcurrently(2) { index ->
                if (index == 0) {
                    attend(session, 1L, attendedAt)
                } else {
                    sessionCommandService.updateSession(updateCommand(session, newLate, times.absentStart))
                }
            }

            val row = attendancePort.findAttendanceBy(session.id!!.value, 1L)!!
            assertThat(row.status).isEqualTo(AttendanceStatus.PRESENT)
            assertThat(row.updatedAt).isNull()
            val reloaded = inReadTransaction { sessionPort.findSessionById(session.id!!.value)!! }
            assertThat(reloaded.attendancePolicy.lateStart).isEqualTo(newLate)
        }
    }

    @Test
    fun `세션 삭제와 인증이 경쟁해도 삭제된 출석 기록이 되살아나지 않는다`() {
        repeat(10) {
            val session = newSession()
            val id = addAttendance(session, memberId = 1L)

            runConcurrently(2) { index ->
                if (index == 0) {
                    attend(session, 1L, times.attendanceStart)
                } else {
                    sessionCommandService.softDeleteSession(session.id!!)
                }
            }

            assertThat(deletedAtOf(id)).isNotNull()
            assertThat(attendancePort.findAttendanceBy(session.id!!.value, 1L)).isNull()
        }
    }

    @Test
    fun `새 세션과 초기 출석 기록은 같은 트랜잭션으로 커밋된다`() {
        val cohortId = newCohort()
        cohortPort.activate(cohortId)
        org.mockito.BDDMockito
            .given(memberQueryUseCase.getMemberIdsByCohortId(cohortId))
            .willReturn(listOf(MemberId(1L), MemberId(2L), MemberId(3L)))

        sessionCommandService.createSession(createCommand())

        val session = inReadTransaction { sessionPort.findAllCohortSessions(cohortId.value).single() }
        assertThat(attendancePort.findAllBySessionId(session.id!!.value).map { it.memberId.value })
            .containsExactlyInAnyOrder(1L, 2L, 3L)
        assertThat(session.attendancePolicy.absentStart).isEqualTo(sessionStart.plus(Duration.ofMinutes(30)))
    }

    @Test
    fun `초기 출석 기록 생성이 실패하면 세션 생성도 롤백된다`() {
        val cohortId = newCohort()
        cohortPort.activate(cohortId)
        org.mockito.BDDMockito
            .given(memberQueryUseCase.getMemberIdsByCohortId(cohortId))
            .willThrow(IllegalStateException("member lookup failed"))

        val failure = runCatching { sessionCommandService.createSession(createCommand()) }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(inReadTransaction { sessionPort.findAllCohortSessions(cohortId.value) }).isEmpty()
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sessions where cohort_id = ?",
                Long::class.javaObjectType,
                cohortId.value,
            ),
        ).isZero()
    }

    @Test
    fun `멤버가 없는 기수도 세션은 만들어진다`() {
        val cohortId = newCohort()
        cohortPort.activate(cohortId)
        org.mockito.BDDMockito
            .given(memberQueryUseCase.getMemberIdsByCohortId(cohortId))
            .willReturn(emptyList())

        sessionCommandService.createSession(createCommand())

        val session = inReadTransaction { sessionPort.findAllCohortSessions(cohortId.value).single() }
        assertThat(attendancePort.findAllBySessionId(session.id!!.value)).isEmpty()
    }

    // 세션 도메인 변환이 지연 로딩 컬렉션(attachments)을 읽으므로 트랜잭션 안에서 읽는다.
    private fun <T> inReadTransaction(block: () -> T): T =
        TransactionTemplate(transactionManager)
            .apply { isReadOnly = true }
            .execute { block() }!!

    private fun createCommand(date: Instant = sessionStart) =
        core.domain.session.port.inbound.command.SessionCreateCommand(
            date = date,
            week = 1,
            place = null,
            eventName = null,
            isOnline = null,
        )

    private fun newCohort(): CohortId = cohortPort.save(Cohort(value = uniqueValue())).id!!

    private fun newSession(
        cohortId: CohortId = newCohort(),
        sessionTimes: SessionAttendanceTimes = times,
    ): Session =
        sessionPort.save(
            Session(
                cohortId = cohortId,
                date = sessionTimes.attendanceStart.plus(Duration.ofMinutes(10)),
                week = 1,
                place = "온라인",
                eventName = "통합 테스트 세션",
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = sessionTimes.attendanceStart,
                        lateStart = sessionTimes.lateStart,
                        absentStart = sessionTimes.absentStart,
                        attendanceCode = CODE,
                    ),
            ),
        )

    private fun addAttendance(
        session: Session,
        memberId: Long,
        status: AttendanceStatus = AttendanceStatus.PENDING,
        updatedAt: Instant? = null,
    ): Long {
        attendancePort.save(
            Attendance(
                sessionId = session.id!!,
                memberId = MemberId(memberId),
                status = status,
                updatedAt = updatedAt,
            ),
        )
        return jdbcTemplate.queryForObject(
            "select attendance_id from attendances where session_id = ? and member_id = ?",
            Long::class.javaObjectType,
            session.id!!.value,
            memberId,
        )!!
    }

    private fun attend(
        session: Session,
        memberId: Long,
        at: Instant,
    ): AttendanceStatus = attendanceCommandService.attendSession(AttendanceRecordCommand(session.id!!, MemberId(memberId), at, CODE))

    private fun updateCommand(
        session: Session,
        lateStart: Instant,
        absentStart: Instant,
    ) = SessionUpdateCommand(
        sessionId = SessionId(session.id!!.value),
        date = session.date,
        week = session.week,
        place = session.place,
        eventName = session.eventName,
        isOnline = session.isOnline,
        attendanceStart = session.attendancePolicy.attendanceStart,
        lateStart = lateStart,
        absentStart = absentStart,
    )

    private fun deletedAtOf(attendanceId: Long): Any? = jdbcTemplate.queryForList("select deleted_at from attendances where attendance_id = ?", attendanceId).single()["deleted_at"]

    private fun uniqueValue(): String = "it-" + UUID.randomUUID().toString().substring(0, 12)

    /** 모든 작업을 동시에 시작하고 결과를 모은다. 교착 등으로 끝나지 않으면 실패한다. */
    private fun <T> runConcurrently(
        count: Int,
        task: (Int) -> T,
    ): List<Result<T>> {
        val ready = CountDownLatch(count)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(count)
        try {
            val futures =
                (0 until count).map { index ->
                    pool.submit(
                        Callable {
                            ready.countDown()
                            start.await()
                            runCatching { task(index) }
                        },
                    )
                }
            ready.await()
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    companion object {
        const val URL_ENV = "DPM_IT_MYSQL_URL"
        private const val USERNAME_ENV = "DPM_IT_MYSQL_USERNAME"
        private const val PASSWORD_ENV = "DPM_IT_MYSQL_PASSWORD"
        private const val CODE = "4321"

        private val SAFE_URL = Regex("^jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/dpm_it[A-Za-z0-9_]*(\\?.*)?$")

        @JvmStatic
        @BeforeAll
        fun requireDisposableLocalDatabase() {
            assumeTrue(SAFE_URL.matches(System.getenv(URL_ENV).orEmpty())) {
                "$URL_ENV 는 로컬 일회용 DB(jdbc:mysql://localhost|127.0.0.1/dpm_it*)만 허용합니다"
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            val url = System.getenv(URL_ENV).orEmpty()
            require(SAFE_URL.matches(url)) { "$URL_ENV 는 로컬 일회용 DB(dpm_it*)만 허용합니다: $url" }
            registry.add("spring.datasource.url") { url }
            registry.add("spring.datasource.username") { System.getenv(USERNAME_ENV) ?: "root" }
            registry.add("spring.datasource.password") { System.getenv(PASSWORD_ENV) ?: "" }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
            registry.add("spring.jpa.properties.hibernate.show_sql") { "false" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "30" }
        }
    }
}
