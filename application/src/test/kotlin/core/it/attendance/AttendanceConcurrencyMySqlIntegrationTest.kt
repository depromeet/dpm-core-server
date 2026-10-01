package core.it.attendance

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.attendance.application.service.AttendanceAutoAbsenceService
import core.application.attendance.application.service.AttendanceCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.session.application.exception.CheckedAttendanceException
import core.application.session.application.service.SessionCommandService
import core.application.session.application.validator.SessionValidator
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
import org.springframework.context.ApplicationEventPublisher
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
 * 실제 MySQL 에서 잠금/조건부 UPDATE 동작을 검증한다. 기본 test 태스크에서는 제외된다.
 *
 * 실행 예:
 *   docker run -d --name dpm-it-mysql -e MYSQL_ROOT_PASSWORD=it -e MYSQL_DATABASE=dpm_it -p 3307:3306 mysql:8.0
 *   DPM_IT_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/dpm_it?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
 *   DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD=it ./gradlew :application:mysqlIntegrationTest
 *
 * 스키마는 엔티티로부터 새로 만든다(ddl-auto=create). 안전을 위해 로컬 호스트의 dpm_it* DB 만 허용한다.
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

    @Autowired lateinit var attendancePolicyProperties: AttendancePolicyProperties

    @Autowired lateinit var autoAbsenceService: AttendanceAutoAbsenceService

    @Autowired lateinit var cohortPort: CohortPersistencePort

    @Autowired lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired lateinit var sessionValidator: SessionValidator

    @Autowired lateinit var cohortQueryService: CohortQueryService

    @Autowired lateinit var sessionPort: SessionPersistencePort

    @Autowired lateinit var attendancePort: AttendancePersistencePort

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean lateinit var sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase

    /** 테스트 데이터의 운영진 결정 시각 */
    private val decidedAt = Instant.parse("2026-01-01T00:00:00Z")
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)

    /** 각 테스트가 시계를 직접 맞추지 않아도 같은 시작 시각에서 출발하도록 한다. */
    @BeforeEach
    fun resetClock() {
        clock.now = Instant.parse("2026-10-01T03:00:00Z")
    }

    // ---------------------------------------------------------------- 인증 경쟁

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
    fun `마감 전에 접수된 인증과 자동 결석이 경쟁해도 최종 상태는 지각이다`() {
        repeat(10) {
            val session = newSession()
            addAttendance(session, memberId = 1L)
            val receivedAt = times.absentStart.minusSeconds(1)
            val closeAt = times.absentStart.plusSeconds(1)

            runConcurrently(2) { index ->
                if (index == 0) {
                    attend(session, 1L, receivedAt)
                } else {
                    attendanceCommandService.closeExpiredAttendances(session.id!!, closeAt)
                }
            }

            val row = attendancePort.findAttendanceBy(session.id!!.value, 1L)!!
            assertThat(row.status).isEqualTo(AttendanceStatus.LATE)
            assertThat(row.attendedAt).isNotNull()
            assertThat(row.updatedAt).isNull()
        }
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
    fun `일괄 운영진 변경은 attendedAt 을 보존하고 자동 결석과 경쟁해도 운영진 값이 남는다`() {
        repeat(5) {
            val session = newSession()
            val memberIds = (1L..20L).toList()
            memberIds.forEach { addAttendance(session, memberId = it) }
            attend(session, 1L, times.attendanceStart)
            val attendedAt = attendancePort.findAttendanceBy(session.id!!.value, 1L)!!.attendedAt

            runConcurrently(2) { index ->
                if (index == 0) {
                    attendanceCommandService.updateAttendanceStatusBulk(
                        session.id!!,
                        AttendanceStatus.EXCUSED_ABSENT,
                        memberIds.reversed().map { MemberId(it) },
                    )
                } else {
                    attendanceCommandService.closeExpiredAttendances(session.id!!, times.absentStart)
                }
            }

            memberIds.forEach { memberId ->
                val row = attendancePort.findAttendanceBy(session.id!!.value, memberId)!!
                assertThat(row.status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
                assertThat(row.updatedAt).isNotNull()
            }
            assertThat(attendancePort.findAttendanceBy(session.id!!.value, 1L)!!.attendedAt).isEqualTo(attendedAt)
        }
    }

    // ---------------------------------------------------------------- 자동 결석

    @Test
    fun `자동 결석은 과거 기수의 오래 전 마감 세션까지 미인증만 처리하고 운영진 결정은 보호하며 반복 실행해도 같다`() {
        val oldCohortId = newCohort()
        val cohortId = newCohort()
        val historic = newSession(oldCohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.minus(Duration.ofDays(400))))
        val eligible = newSession(cohortId, times)
        val future = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.plus(Duration.ofDays(7))))
        val deleted = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.plus(Duration.ofDays(1))))

        val historicPending = addAttendance(historic, memberId = 1L)
        val historicExcused = addAttendance(historic, memberId = 2L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val historicManualPending = addAttendance(historic, memberId = 3L, updatedAt = decidedAt)
        val historicLegacyAbsent = addAttendance(historic, memberId = 4L, status = AttendanceStatus.ABSENT)
        val pending = addAttendance(eligible, memberId = 1L)
        val manualPending = addAttendance(eligible, memberId = 2L, updatedAt = decidedAt)
        addAttendance(eligible, memberId = 3L)
        attend(eligible, 3L, times.lateStart)
        val futurePending = addAttendance(future, memberId = 1L)
        val deletedPending = addAttendance(deleted, memberId = 1L)
        sessionCommandService.softDeleteSession(deleted.id!!)

        clock.now = sessionStart.plus(Duration.ofDays(3)) // 서버가 멈췄다가 며칠 뒤 재시작
        autoAbsenceService.closeExpiredAttendances()
        autoAbsenceService.closeExpiredAttendances()

        assertThat(statusOf(historicPending)).isEqualTo("ABSENT")
        assertThat(autoAbsentAtOf(historicPending)).isNotNull()
        assertThat(updatedAtOf(historicPending)).isNull()
        assertThat(statusOf(historicExcused)).isEqualTo("EXCUSED_ABSENT")
        assertThat(statusOf(historicManualPending)).isEqualTo("PENDING")
        assertThat(statusOf(historicLegacyAbsent)).isEqualTo("ABSENT")
        assertThat(autoAbsentAtOf(historicLegacyAbsent)).isNull()
        assertThat(statusOf(pending)).isEqualTo("ABSENT")
        assertThat(updatedAtOf(pending)).isNull()
        assertThat(statusOf(manualPending)).isEqualTo("PENDING")
        assertThat(attendancePort.findAttendanceBy(eligible.id!!.value, 3L)!!.status).isEqualTo(AttendanceStatus.LATE)
        assertThat(statusOf(futurePending)).isEqualTo("PENDING")
        assertThat(statusOf(deletedPending)).isEqualTo("PENDING")
        assertThat(deletedAtOf(deletedPending)).isNotNull()
    }

    @Test
    fun `자동 결석 후보 조회는 하한 없이 마감 경계를 포함한다`() {
        val cohortId = newCohort()
        val session = newSession(cohortId, times)
        val historic = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.minus(Duration.ofDays(700))))
        addAttendance(session, memberId = 1L)
        addAttendance(historic, memberId = 1L)

        // 모든 기수를 대상으로 조회하므로 다른 테스트의 세션이 함께 나올 수 있다. 이 세션 포함 여부만 본다.
        val before = sessionPort.findSessionIdsToAutoClose(times.absentStart.minusMillis(1))
        val atClose = sessionPort.findSessionIdsToAutoClose(times.absentStart)

        assertThat(before).doesNotContain(session.id)
        assertThat(before).contains(historic.id)
        assertThat(atClose).contains(session.id, historic.id)
    }

    @Test
    fun `마감 연장 시 자동 결석은 다시 인증할 수 있고 수동 결석은 유지된다`() {
        val session = newSession()
        val auto = addAttendance(session, memberId = 1L)
        val manual = addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT, updatedAt = decidedAt)
        attendanceCommandService.closeExpiredAttendances(session.id!!, times.absentStart)
        assertThat(statusOf(auto)).isEqualTo("ABSENT")

        clock.now = times.absentStart.plusSeconds(30)
        sessionCommandService.updateSession(updateCommand(session, times.lateStart, times.absentStart.plus(Duration.ofMinutes(30))))

        assertThat(statusOf(auto)).isEqualTo("PENDING")
        assertThat(updatedAtOf(auto)).isNull()
        assertThat(statusOf(manual)).isEqualTo("ABSENT")
        assertThat(attend(session, 1L, times.absentStart.plusSeconds(40))).isEqualTo(AttendanceStatus.LATE)
    }

    // ---------------------------------------------------------------- 기본 출석 시간 설정

    @Test
    fun `기본 출석 시간 설정은 10_15_30 이다`() {
        assertThat(attendancePolicyProperties.defaultOffsets).isEqualTo(AttendanceTimeOffsets.DEFAULT)
    }

    @Test
    fun `기본값 설정을 바꿔 재시작해도 이전 기수와 현재 기수의 기존 세션은 그대로이고 새 세션에만 적용된다`() {
        val oldCohortId = newCohort()
        val currentCohortId = newCohort()
        val pastStart = sessionStart.minus(Duration.ofDays(60))
        val futureStart = sessionStart.plus(Duration.ofDays(30))

        cohortPort.activate(oldCohortId)
        sessionCommandService.createSession(createCommand(pastStart))
        sessionCommandService.createSession(createCommand(futureStart))
        cohortPort.activate(currentCohortId)
        sessionCommandService.createSession(createCommand(futureStart.plus(Duration.ofDays(1))))

        val existing = inReadTransaction { sessionsOf(listOf(oldCohortId, currentCohortId)) }
        assertThat(existing).hasSize(3)
        existing.forEach {
            assertThat(it.attendancePolicy.absentStart).isEqualTo(it.attendancePolicy.lateStart.plus(Duration.ofMinutes(15)))
        }

        // 같은 DB 에 기본값만 20/5/45 로 바꿔 다시 기동한 서버
        val restarted =
            SessionCommandService(
                sessionPersistencePort = sessionPort,
                eventPublisher = eventPublisher,
                sessionValidator = sessionValidator,
                cohortQueryService = cohortQueryService,
                sentSessionNotificationCommandUseCase = sentSessionNotificationCommandUseCase,
                attendancePolicyProperties = AttendancePolicyProperties(20, 5, 45),
                attendanceCommandService = attendanceCommandService,
                clock = clock,
            )
        val newCurrentStart = futureStart.plus(Duration.ofDays(8))
        inTransaction { restarted.createSession(createCommand(newCurrentStart)) }
        cohortPort.activate(oldCohortId)
        val newOldStart = futureStart.plus(Duration.ofDays(9))
        inTransaction { restarted.createSession(createCommand(newOldStart)) }

        val reloaded = inReadTransaction { sessionsOf(listOf(oldCohortId, currentCohortId)) }.associateBy { it.id }
        existing.forEach { before ->
            val after = reloaded.getValue(before.id)
            assertThat(after.attendancePolicy.attendanceStart).isEqualTo(before.attendancePolicy.attendanceStart)
            assertThat(after.attendancePolicy.lateStart).isEqualTo(before.attendancePolicy.lateStart)
            assertThat(after.attendancePolicy.absentStart).isEqualTo(before.attendancePolicy.absentStart)
        }

        val created = reloaded.values.filter { session -> existing.none { it.id == session.id } }
        assertThat(created.map { it.cohortId }).containsExactlyInAnyOrder(currentCohortId, oldCohortId)
        created.forEach { session ->
            val start = if (session.cohortId == currentCohortId) newCurrentStart else newOldStart
            assertThat(session.attendancePolicy.attendanceStart).isEqualTo(start.minus(Duration.ofMinutes(20)))
            assertThat(session.attendancePolicy.lateStart).isEqualTo(start.plus(Duration.ofMinutes(5)))
            assertThat(session.attendancePolicy.absentStart).isEqualTo(start.plus(Duration.ofMinutes(45)))
        }
    }

    // ---------------------------------------------------------------- 자동 결석 표지

    @Test
    fun `자동 결석은 표지를 남기고 표지 없는 기존 결석은 재개되거나 인증으로 덮이지 않는다`() {
        val session = newSession()
        val auto = addAttendance(session, memberId = 1L)
        val legacy = addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT)

        attendanceCommandService.closeExpiredAttendances(session.id!!, times.absentStart)
        assertThat(autoAbsentAtOf(auto)).isNotNull()
        assertThat(autoAbsentAtOf(legacy)).isNull()

        clock.now = times.absentStart.plusSeconds(30)
        sessionCommandService.updateSession(updateCommand(session, times.lateStart, times.absentStart.plus(Duration.ofMinutes(30))))

        assertThat(statusOf(auto)).isEqualTo("PENDING")
        assertThat(autoAbsentAtOf(auto)).isNull()
        assertThat(statusOf(legacy)).isEqualTo("ABSENT")
        assertThat(runCatching { attend(session, 2L, times.absentStart.plusSeconds(40)) }.exceptionOrNull())
            .isInstanceOf(CheckedAttendanceException::class.java)
        assertThat(statusOf(legacy)).isEqualTo("ABSENT")
    }

    // ---------------------------------------------------------------- 신규 멤버

    @Test
    fun `신규 멤버는 마감 전 세션에만 기록이 생기고 기존 기록은 유지된다`() {
        val cohortId = newCohort()
        val now = Instant.parse("2026-11-01T10:00:00Z")
        clock.now = now
        val expired = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minus(Duration.ofHours(2))))
        val exactDeadline = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minus(Duration.ofMinutes(30))))
        val future = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plus(Duration.ofDays(1))))
        val existing = addAttendance(expired, memberId = 70L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)

        attendanceCommandService.initializeForNewCohortMember(MemberId(70L), cohortId)
        autoAbsenceService.closeExpiredAttendances()

        assertThat(countOf(expired, 70L)).isEqualTo(1L)
        assertThat(statusOf(existing)).isEqualTo("EXCUSED_ABSENT")
        assertThat(countOf(exactDeadline, 70L)).isZero()
        assertThat(countOf(future, 70L)).isEqualTo(1L)
        assertThat(attendancePort.findAttendanceBy(future.id!!.value, 70L)!!.status).isEqualTo(AttendanceStatus.PENDING)
    }

    @Test
    fun `신규 멤버 초기화와 세션 삭제가 경쟁해도 삭제된 세션에 살아 있는 기록이 남지 않는다`() {
        repeat(5) {
            val cohortId = newCohort()
            clock.now = Instant.parse("2026-10-01T03:00:00Z")
            val sessions = (1..3).map { day -> newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.plus(Duration.ofDays(day.toLong())))) }

            runConcurrently(2) { index ->
                if (index == 0) {
                    attendanceCommandService.initializeForNewCohortMember(MemberId(80L), cohortId)
                } else {
                    sessionCommandService.softDeleteSession(sessions[1].id!!)
                }
            }

            assertThat(attendancePort.findAttendanceBy(sessions[1].id!!.value, 80L)).isNull()
            assertThat(countOf(sessions[0], 80L)).isEqualTo(1L)
            assertThat(countOf(sessions[2], 80L)).isEqualTo(1L)
        }
    }

    // ---------------------------------------------------------------- 세션 생성 트랜잭션

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

    // ---------------------------------------------------------------- helpers

    /**
     * 세션 조회는 도메인 변환 시 지연 로딩 컬렉션(attachments)을 읽으므로, 운영 호출자처럼 트랜잭션 안에서 읽는다.
     */
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

    private fun countOf(
        session: Session,
        memberId: Long,
    ): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from attendances where session_id = ? and member_id = ? and deleted_at is null",
            Long::class.javaObjectType,
            session.id!!.value,
            memberId,
        )!!

    private fun autoAbsentAtOf(attendanceId: Long): Any? =
        jdbcTemplate.queryForList("select auto_absent_at from attendances where attendance_id = ?", attendanceId)
            .single()["auto_absent_at"]

    private fun newCohort(): CohortId = cohortPort.save(Cohort(value = uniqueValue())).id!!

    private fun sessionsOf(cohortIds: List<CohortId>): List<Session> =
        cohortIds.flatMap {
            sessionPort.findAllCohortSessions(it.value)
        }

    private fun <T> inTransaction(block: () -> T): T =
        TransactionTemplate(transactionManager)
            .execute { block() }!!

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

    private fun statusOf(attendanceId: Long): String = jdbcTemplate.queryForObject("select status from attendances where attendance_id = ?", String::class.java, attendanceId)!!

    private fun updatedAtOf(attendanceId: Long): Any? = jdbcTemplate.queryForList("select updated_at from attendances where attendance_id = ?", attendanceId).single()["updated_at"]

    private fun deletedAtOf(attendanceId: Long): Any? = jdbcTemplate.queryForList("select deleted_at from attendances where attendance_id = ?", attendanceId).single()["deleted_at"]

    private fun uniqueValue(): String = "it-" + UUID.randomUUID().toString().substring(0, 12)

    /** 모든 작업을 동시에 시작하고 결과(성공/예외)를 모은다. 교착 등으로 끝나지 않으면 실패한다. */
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
