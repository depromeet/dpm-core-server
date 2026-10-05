package core.it.attendance

import core.application.attendance.application.service.AttendanceAutoAbsenceService
import core.application.attendance.application.service.AttendanceCommandService
import core.application.session.application.exception.AttendanceAlreadyDecidedException
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
import java.sql.Timestamp
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

    @Autowired lateinit var autoAbsenceService: AttendanceAutoAbsenceService

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
    fun `마감 전에 접수된 인증은 자동 결석이 먼저 저장되거나 경쟁해도 지각으로 저장된다`() {
        // 순서를 고정한 경우: 자동 결석이 먼저 커밋된 뒤 마감 전에 접수된 요청을 처리
        val sequential = newSession()
        val sequentialId = addAttendance(sequential, memberId = 1L)
        attendanceCommandService.closeExpiredAttendances(sequential.id!!, times.absentStart.plusSeconds(1))
        assertThat(autoAbsentAtOf(sequentialId)).isNotNull()

        assertThat(attend(sequential, 1L, times.absentStart.minusSeconds(1))).isEqualTo(AttendanceStatus.LATE)
        assertThat(statusOf(sequentialId)).isEqualTo("LATE")
        assertThat(autoAbsentAtOf(sequentialId)).isNull()
        assertThat(updatedAtOf(sequentialId)).isNull()

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
            // 인증이 먼저 커밋돼도 운영진 변경이 인증 시각을 지운다
            assertThat(row.attendedAt).isNull()
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
    fun `일괄 운영진 변경은 attendedAt 을 지우고 자동 결석과 경쟁해도 운영진 값이 남는다`() {
        repeat(5) {
            val session = newSession()
            val memberIds = (1L..20L).toList()
            memberIds.forEach { addAttendance(session, memberId = it) }
            attend(session, 1L, times.attendanceStart)
            assertThat(attendancePort.findAttendanceBy(session.id!!.value, 1L)!!.attendedAt).isNotNull()

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
                assertThat(row.attendedAt).isNull()
                assertThat(row.updatedAt).isNotNull()
                assertThat(row.autoAbsentAt).isNull()
            }
        }
    }

    @Test
    fun `자동 결석은 활성 기수의 오래 전 마감 세션까지 현재 PENDING 만 처리하고 지난 기수와 PENDING 이 아닌 기록은 두며 반복 실행해도 같다`() {
        val oldCohortId = newCohort()
        val cohortId = newCohort()
        cohortPort.activate(cohortId)
        val oldCohortSession = newSession(oldCohortId, times)
        val historic = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.minus(Duration.ofDays(400))))
        val eligible = newSession(cohortId, times)
        val future = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.plus(Duration.ofDays(7))))
        val deleted = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.plus(Duration.ofDays(1))))

        val oldCohortPending = addAttendance(oldCohortSession, memberId = 1L)
        val historicPending = addAttendance(historic, memberId = 1L)
        val pending = addAttendance(eligible, memberId = 1L)
        val adminReset = addAttendance(eligible, memberId = 2L, updatedAt = decidedAt)
        val attendedAt = times.lateStart
        val adminResetAfterAttend = addAttendance(eligible, memberId = 3L, updatedAt = decidedAt, attendedAt = attendedAt)
        val attendedPending = addAttendance(eligible, memberId = 4L, attendedAt = attendedAt)
        val adminPresent = addAttendance(eligible, memberId = 5L, status = AttendanceStatus.PRESENT, updatedAt = decidedAt)
        val adminExcused = addAttendance(eligible, memberId = 6L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val legacyAbsent = addAttendance(eligible, memberId = 7L, status = AttendanceStatus.ABSENT)
        addAttendance(eligible, memberId = 8L)
        attend(eligible, 8L, times.lateStart)
        val deletedRow = addAttendance(eligible, memberId = 9L)
        jdbcTemplate.update("update attendances set deleted_at = ? where attendance_id = ?", Timestamp.from(decidedAt), deletedRow)
        val futurePending = addAttendance(future, memberId = 1L)
        val deletedSessionPending = addAttendance(deleted, memberId = 1L)
        sessionCommandService.softDeleteSession(deleted.id!!)

        clock.now = sessionStart.plus(Duration.ofDays(3)) // 서버가 멈췄다가 며칠 뒤 재시작
        val first = autoAbsenceService.closeExpiredAttendances()
        val second = autoAbsenceService.closeExpiredAttendances()

        assertThat(first).isEqualTo(5)
        assertThat(second).isZero()
        assertThat(statusOf(oldCohortPending)).isEqualTo("PENDING")
        assertThat(autoAbsentAtOf(oldCohortPending)).isNull()
        listOf(historicPending, pending, adminReset, adminResetAfterAttend, attendedPending).forEach {
            assertThat(statusOf(it)).isEqualTo("ABSENT")
            assertThat(autoAbsentAtOf(it)).isNotNull()
        }
        assertThat(updatedAtOf(pending)).isNull()
        assertThat(updatedAtOf(adminReset)).isNotNull()
        assertThat(updatedAtOf(adminResetAfterAttend)).isNotNull()
        assertThat(attendancePort.findAttendanceBy(eligible.id!!.value, 3L)!!.attendedAt).isEqualTo(attendedAt)
        assertThat(statusOf(adminPresent)).isEqualTo("PRESENT")
        assertThat(statusOf(adminExcused)).isEqualTo("EXCUSED_ABSENT")
        assertThat(statusOf(legacyAbsent)).isEqualTo("ABSENT")
        assertThat(autoAbsentAtOf(legacyAbsent)).isNull()
        assertThat(attendancePort.findAttendanceBy(eligible.id!!.value, 8L)!!.status).isEqualTo(AttendanceStatus.LATE)
        assertThat(statusOf(deletedRow)).isEqualTo("PENDING")
        assertThat(autoAbsentAtOf(deletedRow)).isNull()
        assertThat(statusOf(futurePending)).isEqualTo("PENDING")
        assertThat(statusOf(deletedSessionPending)).isEqualTo("PENDING")
        assertThat(deletedAtOf(deletedSessionPending)).isNotNull()
    }

    @Test
    fun `활성 기수가 없으면 자동 결석을 건너뛴다`() {
        val cohortId = newCohort()
        val session = newSession(cohortId, times)
        val id = addAttendance(session, memberId = 1L)
        clock.now = times.absentStart.plusSeconds(1)
        cohortPort.deactivateAll()

        assertThat(autoAbsenceService.closeExpiredAttendances()).isZero()
        assertThat(statusOf(id)).isEqualTo("PENDING")
    }

    @Test
    fun `자동 결석 후보 조회는 기수와 PENDING 기준이고 하한 없이 마감 경계를 포함한다`() {
        val cohortId = newCohort()
        val otherCohortId = newCohort()
        val session = newSession(cohortId, times)
        val historic = newSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.minus(Duration.ofDays(700))))
        val adminResetOnly = newSession(cohortId, times)
        val decidedOnly = newSession(cohortId, times)
        val deletedRowOnly = newSession(cohortId, times)
        val otherCohort = newSession(otherCohortId, times)
        addAttendance(session, memberId = 1L)
        addAttendance(historic, memberId = 1L)
        addAttendance(adminResetOnly, memberId = 1L, updatedAt = decidedAt, attendedAt = times.lateStart)
        addAttendance(decidedOnly, memberId = 1L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val deletedRow = addAttendance(deletedRowOnly, memberId = 1L)
        jdbcTemplate.update("update attendances set deleted_at = ? where attendance_id = ?", Timestamp.from(decidedAt), deletedRow)
        addAttendance(otherCohort, memberId = 1L)

        val before = sessionPort.findSessionIdsToAutoClose(cohortId, times.absentStart.minusMillis(1))
        val atClose = sessionPort.findSessionIdsToAutoClose(cohortId, times.absentStart)

        assertThat(before).containsExactly(historic.id)
        assertThat(atClose).containsExactly(historic.id, session.id, adminResetOnly.id)
    }

    @Test
    fun `운영진이 PENDING 으로 되돌리는 것과 자동 결석이 경쟁해도 다음 실행 뒤에는 결석이고 운영진 표지는 남는다`() {
        repeat(10) {
            val session = newSession()
            val id = addAttendance(session, memberId = 1L, status = AttendanceStatus.PRESENT, updatedAt = decidedAt)
            val closeAt = times.absentStart.plusSeconds(1)
            clock.now = closeAt

            val results =
                runConcurrently(2) { index ->
                    if (index == 0) {
                        attendanceCommandService.updateAttendanceStatus(
                            AttendanceStatusUpdateCommand(session.id!!, MemberId(1L), AttendanceStatus.PENDING),
                        )
                    } else {
                        attendanceCommandService.closeExpiredAttendances(session.id!!, closeAt)
                    }
                }
            assertThat(results).allMatch { it.isSuccess }
            // 자동 결석이 먼저면 PRESENT 라 건너뛰고 PENDING 으로 남는다. 다음 실행에서 결석이 된다.
            attendanceCommandService.closeExpiredAttendances(session.id!!, closeAt)

            assertThat(statusOf(id)).isEqualTo("ABSENT")
            assertThat(autoAbsentAtOf(id)).isNotNull()
            assertThat(updatedAtOf(id)).isNotNull()
        }
    }

    @Test
    fun `운영진이 PENDING 으로 되돌린 기록을 출석으로 바꾸는 것과 자동 결석이 경쟁해도 운영진 출석이 남는다`() {
        repeat(10) {
            val session = newSession()
            val id = addAttendance(session, memberId = 1L, updatedAt = decidedAt)
            val closeAt = times.absentStart.plusSeconds(1)
            clock.now = closeAt

            val results =
                runConcurrently(2) { index ->
                    if (index == 0) {
                        attendanceCommandService.updateAttendanceStatus(
                            AttendanceStatusUpdateCommand(session.id!!, MemberId(1L), AttendanceStatus.PRESENT),
                        )
                    } else {
                        attendanceCommandService.closeExpiredAttendances(session.id!!, closeAt)
                    }
                }
            assertThat(results).allMatch { it.isSuccess }

            assertThat(statusOf(id)).isEqualTo("PRESENT")
            assertThat(autoAbsentAtOf(id)).isNull()
        }
    }

    @Test
    fun `운영진이 PENDING 으로 되돌린 기록의 자동 결석은 마감 연장으로 재개되지 않고 인증으로 덮어쓰지 않는다`() {
        val session = newSession()
        val id = addAttendance(session, memberId = 1L, updatedAt = decidedAt)

        attendanceCommandService.closeExpiredAttendances(session.id!!, times.absentStart)
        assertThat(statusOf(id)).isEqualTo("ABSENT")

        // 마감 전에 접수된 요청이 늦게 도착해도 운영진 기록이라 저장하지 않는다(PENDING 이던 때도 같다).
        assertThat(runCatching { attend(session, 1L, times.absentStart.minusSeconds(1)) }.exceptionOrNull())
            .isInstanceOf(AttendanceAlreadyDecidedException::class.java)

        clock.now = times.absentStart.plusSeconds(30)
        sessionCommandService.updateSession(updateCommand(session, times.lateStart, times.absentStart.plus(Duration.ofMinutes(30))))

        assertThat(statusOf(id)).isEqualTo("ABSENT")
        assertThat(autoAbsentAtOf(id)).isNotNull()
    }

    @Test
    fun `마감 연장 시 표지가 있는 자동 결석만 다시 인증할 수 있고 수동 결석과 기존 결석은 유지된다`() {
        val session = newSession()
        val auto = addAttendance(session, memberId = 1L)
        val legacy = addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT)
        val manual = addAttendance(session, memberId = 3L, status = AttendanceStatus.ABSENT, updatedAt = decidedAt)

        attendanceCommandService.closeExpiredAttendances(session.id!!, times.absentStart)
        assertThat(autoAbsentAtOf(auto)).isNotNull()
        assertThat(autoAbsentAtOf(legacy)).isNull()

        clock.now = times.absentStart.plusSeconds(30)
        sessionCommandService.updateSession(updateCommand(session, times.lateStart, times.absentStart.plus(Duration.ofMinutes(30))))

        assertThat(statusOf(auto)).isEqualTo("PENDING")
        assertThat(autoAbsentAtOf(auto)).isNull()
        assertThat(updatedAtOf(auto)).isNull()
        assertThat(statusOf(manual)).isEqualTo("ABSENT")
        assertThat(statusOf(legacy)).isEqualTo("ABSENT")

        assertThat(attend(session, 1L, times.absentStart.plusSeconds(40))).isEqualTo(AttendanceStatus.LATE)
        assertThat(runCatching { attend(session, 2L, times.absentStart.plusSeconds(40)) }.exceptionOrNull())
            .isInstanceOf(CheckedAttendanceException::class.java)
        assertThat(statusOf(legacy)).isEqualTo("ABSENT")
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

    private fun autoAbsentAtOf(attendanceId: Long): Any? =
        jdbcTemplate.queryForList("select auto_absent_at from attendances where attendance_id = ?", attendanceId)
            .single()["auto_absent_at"]

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
        attendedAt: Instant? = null,
    ): Long {
        attendancePort.save(
            Attendance(
                sessionId = session.id!!,
                memberId = MemberId(memberId),
                status = status,
                attendedAt = attendedAt,
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
