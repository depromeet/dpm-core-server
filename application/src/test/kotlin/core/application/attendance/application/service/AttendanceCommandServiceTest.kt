package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.session.application.exception.AttendanceAlreadyDecidedException
import core.application.session.application.exception.AttendanceClosedException
import core.application.session.application.exception.CheckedAttendanceException
import core.application.session.application.exception.InvalidAttendanceCodeException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.application.exception.TooEarlyAttendanceException
import core.application.support.AttendanceTestFixture
import core.application.support.AttendanceTestFixture.Companion.CODE
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AttendanceCommandServiceTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)
    private val fixture = AttendanceTestFixture(now = sessionStart)
    private val cohortId = fixture.createActiveCohort()
    private val session: Session = fixture.createSession(cohortId, times)
    private val sessionId = session.id!!

    // ---------------------------------------------------------------- 인증

    @Test
    fun `정확히 인증 시작이면 출석으로 저장한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        val status = attend(1L, times.attendanceStart)

        assertThat(status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(id).attendedAt).isEqualTo(times.attendanceStart)
        assertThat(fixture.attendances.row(id).updatedAt).isNull()
    }

    @Test
    fun `지각 시작 1ns 전은 출석, 정확히 지각 시작은 지각으로 저장한다`() {
        val present = fixture.addAttendance(session, memberId = 1L)
        val late = fixture.addAttendance(session, memberId = 2L)

        assertThat(attend(1L, times.lateStart.minusNanos(1))).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(attend(2L, times.lateStart)).isEqualTo(AttendanceStatus.LATE)

        assertThat(fixture.attendances.row(present).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(late).status).isEqualTo(AttendanceStatus.LATE)
    }

    @Test
    fun `마감 1ns 전은 지각으로 저장한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        assertThat(attend(1L, times.absentStart.minusNanos(1))).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(id).attendedAt).isEqualTo(times.absentStart.minusNanos(1))
    }

    @Test
    fun `정확히 마감 시각이면 마감 오류이고 아무것도 저장하지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        assertThatThrownBy { attend(1L, times.absentStart) }.isInstanceOf(AttendanceClosedException::class.java)
        assertThatThrownBy { attend(1L, times.absentStart.plusSeconds(3600)) }
            .isInstanceOf(AttendanceClosedException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(id).attendedAt).isNull()
        assertThat(fixture.attendances.writeCalls).doesNotContain("recordAttendanceIfAllowed")
    }

    @Test
    fun `인증 시작 전이면 너무 이름 오류이고 저장하지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        assertThatThrownBy { attend(1L, times.attendanceStart.minusNanos(1)) }
            .isInstanceOf(TooEarlyAttendanceException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.writeCalls).isEmpty()
    }

    @Test
    fun `코드가 틀리면 상태를 바꾸지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        assertThatThrownBy { attend(1L, times.attendanceStart, code = "0000") }
            .isInstanceOf(InvalidAttendanceCodeException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(id).attendedAt).isNull()
        assertThat(fixture.attendances.writeCalls).isEmpty()
    }

    @Test
    fun `코드가 틀리면 자동 결석도 바꾸지 않는다`() {
        val id =
            fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.ABSENT, autoAbsentAt = times.absentStart)

        assertThatThrownBy { attend(1L, times.lateStart, code = "0000") }
            .isInstanceOf(InvalidAttendanceCodeException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).autoAbsentAt).isEqualTo(times.absentStart)
    }

    @Test
    fun `표지 없는 기존 결석은 기록이 비어 있어도 인증으로 덮어쓰지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.ABSENT)

        assertThatThrownBy { attend(1L, times.attendanceStart) }.isInstanceOf(CheckedAttendanceException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).attendedAt).isNull()
        assertThat(fixture.attendances.writeCalls).isEmpty()
    }

    @Test
    fun `표지 없는 기존 결석은 저장소 조건에서도 인증으로 바뀌지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.ABSENT)

        assertThat(fixture.attendances.recordAttendanceIfAllowed(id, AttendanceStatus.PRESENT, times.attendanceStart))
            .isFalse()
        assertThat(fixture.attendances.reopenAutoAbsence(id)).isFalse()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `이미 인증했으면 다시 인증할 수 없다`() {
        fixture.addAttendance(session, memberId = 1L)
        attend(1L, times.attendanceStart)

        assertThatThrownBy { attend(1L, times.lateStart) }.isInstanceOf(CheckedAttendanceException::class.java)
        assertThat(fixture.attendances.rowOf(sessionId.value, 1L).status).isEqualTo(AttendanceStatus.PRESENT)
    }

    @Test
    fun `운영진이 정한 상태는 attendedAt 이 없어도 인증으로 덮어쓰지 않는다`() {
        val decidedAt = sessionStart.minusSeconds(3600)
        val excused =
            fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val manualAbsent =
            fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT, updatedAt = decidedAt)
        val manualPending =
            fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.PENDING, updatedAt = decidedAt)

        listOf(1L, 2L, 3L).forEach { memberId ->
            assertThatThrownBy { attend(memberId, times.attendanceStart) }
                .isInstanceOf(AttendanceAlreadyDecidedException::class.java)
        }

        assertThat(fixture.attendances.row(excused).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(manualAbsent).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(manualPending).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(excused).attendedAt).isNull()
    }

    @Test
    fun `마감 전에 받은 요청은 자동 결석이 먼저 저장됐어도 정상 판정으로 저장한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        val receivedAt = times.absentStart.minusSeconds(1)

        // 요청 처리 전에 스케줄러가 먼저 마감 처리
        fixture.clock.now = times.absentStart.plusSeconds(5)
        fixture.attendanceCommandService.closeExpiredAttendances(sessionId, fixture.clock.now)
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).autoAbsentAt).isEqualTo(fixture.clock.now)

        val status = attend(1L, receivedAt)

        assertThat(status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(id).attendedAt).isEqualTo(receivedAt)
        assertThat(fixture.attendances.row(id).updatedAt).isNull()
        assertThat(fixture.attendances.row(id).autoAbsentAt).isNull()
    }

    @Test
    fun `출석 기록이 없으면 404 이고 삭제된 세션이면 세션 없음이다`() {
        assertThatThrownBy { attend(99L, times.attendanceStart) }.isInstanceOf(AttendanceNotFoundException::class.java)

        fixture.addAttendance(session, memberId = 1L)
        fixture.sessionCommandService.softDeleteSession(sessionId)

        assertThatThrownBy { attend(1L, times.attendanceStart) }.isInstanceOf(SessionNotFoundException::class.java)
    }

    @Test
    fun `인증은 세션 공유 잠금을 먼저 잡는다`() {
        fixture.addAttendance(session, memberId = 1L)

        attend(1L, times.attendanceStart)

        assertThat(fixture.sessions.lockCalls).containsExactly("share:${sessionId.value}")
    }

    @Test
    fun `같은 멤버의 동시 인증은 하나만 저장되고 나머지는 이미 출석 오류다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        val threads = 16
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)

        val futures =
            (0 until threads).map { index ->
                pool.submit(
                    Callable {
                        ready.countDown()
                        start.await()
                        runCatching { attend(1L, times.attendanceStart.plusMillis(index.toLong())) }
                    },
                )
            }
        ready.await()
        start.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        val successes = results.filter { it.isSuccess }
        assertThat(successes).hasSize(1)
        assertThat(results.filter { it.isFailure }.map { it.exceptionOrNull() })
            .allMatch { it is CheckedAttendanceException }
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PRESENT)
    }

    // ---------------------------------------------------------------- 운영진 변경

    @Test
    fun `운영진 단건 변경은 attendedAt 을 보존하고 updatedAt 을 기록한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        attend(1L, times.lateStart)
        fixture.clock.now = times.absentStart.plusSeconds(600)

        fixture.attendanceCommandService.updateAttendanceStatus(
            AttendanceStatusUpdateCommand(sessionId, MemberId(1L), AttendanceStatus.PRESENT),
        )

        val row = fixture.attendances.row(id)
        assertThat(row.status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(row.attendedAt).isEqualTo(times.lateStart)
        assertThat(row.updatedAt).isEqualTo(fixture.clock.now)
    }

    @Test
    fun `운영진 단건 변경 대상이 없으면 404`() {
        assertThatThrownBy {
            fixture.attendanceCommandService.updateAttendanceStatus(
                AttendanceStatusUpdateCommand(sessionId, MemberId(77L), AttendanceStatus.ABSENT),
            )
        }.isInstanceOf(AttendanceNotFoundException::class.java)
    }

    @Test
    fun `운영진 일괄 변경은 attendedAt 을 보존한다`() {
        val attended = fixture.addAttendance(session, memberId = 1L)
        val pending = fixture.addAttendance(session, memberId = 2L)
        attend(1L, times.attendanceStart)

        fixture.attendanceCommandService.updateAttendanceStatusBulk(
            sessionId,
            AttendanceStatus.LATE,
            listOf(MemberId(2L), MemberId(1L), MemberId(1L)),
        )

        assertThat(fixture.attendances.row(attended).status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(attended).attendedAt).isEqualTo(times.attendanceStart)
        assertThat(fixture.attendances.row(attended).updatedAt).isNotNull()
        assertThat(fixture.attendances.row(pending).status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(pending).attendedAt).isNull()
    }

    @Test
    fun `일괄 변경 대상 중 하나라도 없으면 아무것도 바꾸지 않는다`() {
        val existing = fixture.addAttendance(session, memberId = 1L)

        assertThatThrownBy {
            fixture.attendanceCommandService.updateAttendanceStatusBulk(
                sessionId,
                AttendanceStatus.ABSENT,
                listOf(MemberId(1L), MemberId(404L)),
            )
        }.isInstanceOf(AttendanceNotFoundException::class.java)

        assertThat(fixture.attendances.row(existing).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(existing).updatedAt).isNull()
    }

    @Test
    fun `결석 사유 승인 같은 운영진 인정 결석은 이후 인증과 자동 결석으로 바뀌지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.attendanceCommandService.updateAttendanceStatus(
            AttendanceStatusUpdateCommand(sessionId, MemberId(1L), AttendanceStatus.EXCUSED_ABSENT),
        )

        assertThatThrownBy { attend(1L, times.attendanceStart) }
            .isInstanceOf(AttendanceAlreadyDecidedException::class.java)
        fixture.clock.now = times.absentStart
        fixture.attendanceCommandService.closeExpiredAttendances(sessionId, fixture.clock.now)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
    }

    // ---------------------------------------------------------------- 자동 결석

    @Test
    fun `마감 1ns 전에는 자동 결석하지 않고 정확히 마감부터 처리한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        val before =
            fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart.minusNanos(1))
        assertThat(before).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)

        val atClose = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)
        assertThat(atClose).isEqualTo(1)
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).updatedAt).isNull()
        assertThat(fixture.attendances.row(id).autoAbsentAt).isEqualTo(times.absentStart)
    }

    @Test
    fun `운영진 변경은 자동 결석 표지를 해제한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)

        fixture.attendanceCommandService.updateAttendanceStatus(
            AttendanceStatusUpdateCommand(sessionId, MemberId(1L), AttendanceStatus.ABSENT),
        )

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).autoAbsentAt).isNull()
        assertThat(fixture.attendances.row(id).updatedAt).isNotNull()
        assertThatThrownBy { attend(1L, times.attendanceStart) }
            .isInstanceOf(AttendanceAlreadyDecidedException::class.java)
    }

    // ---------------------------------------------------------------- 신규 멤버

    @Test
    fun `신규 멤버는 마감이 지나지 않은 세션에만 출석 기록이 생긴다`() {
        val now = Instant.parse("2026-10-20T10:00:00Z")
        fixture.clock.now = now
        val expired = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minusSeconds(7200)))
        val exactDeadline =
            fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minusSeconds(30 * 60)))
        val justOpen =
            fixture.createSession(
                cohortId,
                AttendanceTimeOffsets.DEFAULT.resolveFor(now.minusSeconds(30 * 60).plusNanos(1)),
            )
        val future = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(86_400)))

        fixture.attendanceCommandService.initializeForNewCohortMember(MemberId(50L), cohortId)

        val created = fixture.attendances.all().filter { it.memberId == 50L }.map { it.sessionId }
        assertThat(created).containsExactlyInAnyOrder(justOpen.id!!.value, future.id!!.value)
        assertThat(created).doesNotContain(expired.id!!.value, exactDeadline.id!!.value, sessionId.value)
        assertThat(fixture.attendances.all().filter { it.memberId == 50L }).allMatch { it.status == AttendanceStatus.PENDING }
    }

    @Test
    fun `신규 멤버 초기화는 기존 기록을 그대로 두고 세션을 ID 순서로 잠근다`() {
        val now = Instant.parse("2026-10-20T10:00:00Z")
        fixture.clock.now = now
        val first = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(3600)))
        val second = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(7200)))
        val existing =
            fixture.addAttendance(first, memberId = 50L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = now)
        val expiredExisting = fixture.addAttendance(session, memberId = 50L, status = AttendanceStatus.PRESENT)

        fixture.attendanceCommandService.initializeForNewCohortMember(MemberId(50L), cohortId)

        val rows = fixture.attendances.all().filter { it.memberId == 50L }
        assertThat(rows).hasSize(3)
        assertThat(fixture.attendances.row(existing).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(expiredExisting).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(rows.single { it.sessionId == second.id!!.value }.status).isEqualTo(AttendanceStatus.PENDING)

        val lockedIds = fixture.sessions.lockCalls.map { it.substringAfter("share:").toLong() }
        assertThat(lockedIds).isSorted()
        assertThat(lockedIds).contains(sessionId.value, first.id!!.value, second.id!!.value)
    }

    @Test
    fun `신규 멤버 초기화 뒤 자동 결석이 실행돼도 가입 전 마감 세션의 결석이 생기지 않는다`() {
        val now = Instant.parse("2026-10-20T10:00:00Z")
        fixture.clock.now = now

        fixture.attendanceCommandService.initializeForNewCohortMember(MemberId(50L), cohortId)
        fixture.autoAbsenceService.closeExpiredAttendances()

        assertThat(fixture.attendances.all().filter { it.memberId == 50L && it.status == AttendanceStatus.ABSENT })
            .isEmpty()
    }

    @Test
    fun `멤버가 없는 기수의 새 세션은 출석 기록 없이 끝난다`() {
        fixture.attendanceCommandService.createAttendances(sessionId, cohortId)

        assertThat(fixture.attendances.all()).isEmpty()
    }

    @Test
    fun `자동 결석은 미인증 중 운영진 변경이 없는 기록만 바꾼다`() {
        val decidedAt = sessionStart.minusSeconds(60)
        val pending = fixture.addAttendance(session, memberId = 1L)
        val present = fixture.addAttendance(session, memberId = 2L)
        attend(2L, times.attendanceStart)
        val manualPending =
            fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.PENDING, updatedAt = decidedAt)
        val excused =
            fixture.addAttendance(session, memberId = 4L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = decidedAt)
        val legacyAttendedPending =
            fixture.addAttendance(session, memberId = 5L, status = AttendanceStatus.PENDING, attendedAt = sessionStart)

        val closed = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(pending).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(present).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(manualPending).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(excused).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(legacyAttendedPending).status).isEqualTo(AttendanceStatus.PENDING)
    }

    @Test
    fun `자동 결석을 반복 실행해도 결과가 같다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        val first = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)
        val second = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)

        assertThat(first).isEqualTo(1)
        assertThat(second).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `오래 전에 마감된 과거 기수 세션의 미인증도 자동 결석하고 운영진 결정은 그대로 둔다`() {
        val historic =
            fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart.minus(java.time.Duration.ofDays(400))))
        val pending = fixture.addAttendance(historic, memberId = 1L)
        val excused =
            fixture.addAttendance(historic, memberId = 2L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = sessionStart)
        val manualPending = fixture.addAttendance(historic, memberId = 3L, updatedAt = sessionStart)
        val legacyAbsent = fixture.addAttendance(historic, memberId = 4L, status = AttendanceStatus.ABSENT)

        val closed = fixture.attendanceCommandService.closeExpiredAttendances(historic.id!!, sessionStart)

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(pending).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(pending).autoAbsentAt).isEqualTo(sessionStart)
        assertThat(fixture.attendances.row(excused).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(manualPending).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(legacyAbsent).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(legacyAbsent).autoAbsentAt).isNull()
    }

    @Test
    fun `삭제된 세션은 자동 결석하지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.sessionCommandService.softDeleteSession(sessionId)

        val closed = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)

        assertThat(closed).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(id).deletedAt).isNotNull()
    }

    @Test
    fun `자동 결석은 잠근 뒤 읽은 최신 마감을 다시 확인한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)
        val extended = times.absentStart.plusSeconds(600)
        // 스케줄러가 후보를 고른 뒤 운영진이 마감을 연장한 상황
        fixture.sessionCommandService.updateSession(updateCommandFor(session, times.lateStart, extended))

        val closed = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)

        assertThat(closed).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
    }

    private fun attend(
        memberId: Long,
        at: Instant,
        code: String = CODE,
    ): AttendanceStatus =
        fixture.attendanceCommandService.attendSession(
            AttendanceRecordCommand(
                sessionId = sessionId,
                memberId = MemberId(memberId),
                attendedAt = at,
                attendanceCode = code,
            ),
        )

    private fun updateCommandFor(
        session: Session,
        lateStart: Instant,
        absentStart: Instant,
    ) = core.domain.session.port.inbound.command.SessionUpdateCommand(
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
}
