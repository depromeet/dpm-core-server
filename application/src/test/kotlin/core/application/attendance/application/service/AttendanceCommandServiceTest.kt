package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.session.application.exception.AttendanceAlreadyDecidedException
import core.application.session.application.exception.AttendanceClosedException
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
import core.domain.session.port.inbound.command.SessionUpdateCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class AttendanceCommandServiceTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)
    private val fixture = AttendanceTestFixture(now = sessionStart)
    private val cohortId = fixture.createActiveCohort()
    private val session: Session = fixture.createSession(cohortId, times)
    private val sessionId = session.id!!

    @Test
    fun `경계 시각에 맞춰 출석과 지각으로 요청 시각을 저장한다`() {
        val cases =
            listOf(
                times.attendanceStart to AttendanceStatus.PRESENT,
                times.lateStart.minusNanos(1) to AttendanceStatus.PRESENT,
                times.lateStart to AttendanceStatus.LATE,
                times.absentStart.minusNanos(1) to AttendanceStatus.LATE,
            )

        cases.forEachIndexed { index, (at, expected) ->
            val memberId = index + 1L
            val id = fixture.addAttendance(session, memberId = memberId)

            assertThat(attend(memberId, at)).isEqualTo(expected)
            val row = fixture.attendances.row(id)
            assertThat(row.status).isEqualTo(expected)
            assertThat(row.attendedAt).isEqualTo(at)
            assertThat(row.updatedAt).isNull()
        }
    }

    @Test
    fun `정확히 마감 시각부터는 마감 오류이고 아무것도 저장하지 않는다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        assertThatThrownBy { attend(1L, times.absentStart) }.isInstanceOf(AttendanceClosedException::class.java)
        assertThatThrownBy { attend(1L, times.absentStart.plusSeconds(3600)) }
            .isInstanceOf(AttendanceClosedException::class.java)

        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(id).attendedAt).isNull()
    }

    @Test
    fun `너무 이르거나 코드가 틀린 요청은 아무것도 바꾸지 않는다`() {
        val pending = fixture.addAttendance(session, memberId = 1L)
        val autoAbsent =
            fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT, autoAbsentAt = times.absentStart)

        assertThatThrownBy { attend(1L, times.attendanceStart.minusNanos(1)) }
            .isInstanceOf(TooEarlyAttendanceException::class.java)
        assertThatThrownBy { attend(1L, times.attendanceStart, code = "0000") }
            .isInstanceOf(InvalidAttendanceCodeException::class.java)
        assertThatThrownBy { attend(2L, times.lateStart, code = "0000") }
            .isInstanceOf(InvalidAttendanceCodeException::class.java)

        assertThat(fixture.attendances.row(pending).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(pending).attendedAt).isNull()
        assertThat(fixture.attendances.row(autoAbsent).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(autoAbsent).autoAbsentAt).isEqualTo(times.absentStart)
        assertThat(fixture.attendances.row(autoAbsent).attendedAt).isNull()
    }

    @Test
    fun `운영진이 정한 상태는 attendedAt 이 없어도 인증으로 덮어쓰지 않는다`() {
        val decidedAt = sessionStart.minusSeconds(3600)
        val decided =
            listOf(AttendanceStatus.EXCUSED_ABSENT, AttendanceStatus.ABSENT, AttendanceStatus.PENDING)
                .mapIndexed { index, status ->
                    fixture.addAttendance(session, memberId = index + 1L, status = status, updatedAt = decidedAt) to status
                }

        decided.forEachIndexed { index, (id, status) ->
            assertThatThrownBy { attend(index + 1L, times.attendanceStart) }
                .isInstanceOf(AttendanceAlreadyDecidedException::class.java)
            assertThat(fixture.attendances.row(id).status).isEqualTo(status)
            assertThat(fixture.attendances.row(id).attendedAt).isNull()
        }
    }

    @Test
    fun `출석 기록이 없으면 404 이고 삭제된 세션이면 세션 없음이다`() {
        assertThatThrownBy { attend(99L, times.attendanceStart) }.isInstanceOf(AttendanceNotFoundException::class.java)

        fixture.addAttendance(session, memberId = 1L)
        fixture.sessionCommandService.softDeleteSession(sessionId)

        assertThatThrownBy { attend(1L, times.attendanceStart) }.isInstanceOf(SessionNotFoundException::class.java)
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
    fun `운영진 단건 변경은 인증 시각과 자동 결석 표지를 지우고 변경 시각을 남긴다`() {
        val attended = fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PRESENT, attendedAt = times.attendanceStart)
        val autoAbsent = fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT, autoAbsentAt = times.absentStart)
        // 인증 시각이 남아 있는 예전 운영진 변경 기록도 다시 바꾸면 지운다
        val legacyManual =
            fixture.addAttendance(
                session,
                memberId = 3L,
                status = AttendanceStatus.LATE,
                attendedAt = times.lateStart,
                updatedAt = sessionStart.minusSeconds(60),
            )

        listOf(1L to AttendanceStatus.LATE, 2L to AttendanceStatus.EXCUSED_ABSENT, 3L to AttendanceStatus.PRESENT).forEach { (memberId, status) ->
            fixture.attendanceCommandService.updateAttendanceStatus(AttendanceStatusUpdateCommand(sessionId, MemberId(memberId), status))
        }

        listOf(attended, autoAbsent, legacyManual).forEach { id ->
            val row = fixture.attendances.row(id)
            assertThat(row.attendedAt).isNull()
            assertThat(row.updatedAt).isEqualTo(sessionStart)
            assertThat(row.autoAbsentAt).isNull()
        }
        assertThat(listOf(attended, autoAbsent, legacyManual).map { fixture.attendances.row(it).status })
            .containsExactly(AttendanceStatus.LATE, AttendanceStatus.EXCUSED_ABSENT, AttendanceStatus.PRESENT)
        // 운영진 기록이라 이후 인증으로 덮어쓰지 않는다
        assertThatThrownBy { attend(1L, times.attendanceStart) }.isInstanceOf(AttendanceAlreadyDecidedException::class.java)
    }

    @Test
    fun `운영진 일괄 변경은 대상 모두의 인증 시각을 지우고 다른 멤버 기록은 두며 실패하면 아무것도 지우지 않는다`() {
        val first = fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PRESENT, attendedAt = times.attendanceStart)
        val second = fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.LATE, attendedAt = times.lateStart)
        val untouched = fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.PRESENT, attendedAt = times.attendanceStart)

        assertThatThrownBy {
            fixture.attendanceCommandService.updateAttendanceStatusBulk(sessionId, AttendanceStatus.ABSENT, listOf(MemberId(1L), MemberId(404L)))
        }.isInstanceOf(AttendanceNotFoundException::class.java)
        assertThat(fixture.attendances.row(first).attendedAt).isEqualTo(times.attendanceStart)

        fixture.attendanceCommandService.updateAttendanceStatusBulk(sessionId, AttendanceStatus.EXCUSED_ABSENT, listOf(MemberId(2L), MemberId(1L)))

        listOf(first, second).forEach { id ->
            assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
            assertThat(fixture.attendances.row(id).attendedAt).isNull()
            assertThat(fixture.attendances.row(id).updatedAt).isEqualTo(sessionStart)
        }
        assertThat(fixture.attendances.row(untouched).attendedAt).isEqualTo(times.attendanceStart)
        assertThat(fixture.attendances.row(untouched).updatedAt).isNull()
    }

    @Test
    fun `운영진 일괄 변경은 중복 멤버를 한 번만 반영하고 대상 중 하나라도 없으면 아무것도 바꾸지 않는다`() {
        val first = fixture.addAttendance(session, memberId = 1L)
        val second = fixture.addAttendance(session, memberId = 2L)

        assertThatThrownBy {
            fixture.attendanceCommandService.updateAttendanceStatusBulk(
                sessionId,
                AttendanceStatus.ABSENT,
                listOf(MemberId(1L), MemberId(404L)),
            )
        }.isInstanceOf(AttendanceNotFoundException::class.java)
        assertThat(fixture.attendances.row(first).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(first).updatedAt).isNull()

        fixture.attendanceCommandService.updateAttendanceStatusBulk(
            sessionId,
            AttendanceStatus.LATE,
            listOf(MemberId(2L), MemberId(1L), MemberId(1L)),
        )
        listOf(first, second).forEach { id ->
            assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.LATE)
            assertThat(fixture.attendances.row(id).updatedAt).isNotNull()
        }
    }

    @Test
    fun `마감 1ns 전에는 자동 결석하지 않고 정확히 마감부터 표지와 함께 처리한다`() {
        val id = fixture.addAttendance(session, memberId = 1L)

        val before = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart.minusNanos(1))
        assertThat(before).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)

        val atClose = fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)
        assertThat(atClose).isEqualTo(1)
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).updatedAt).isNull()
        assertThat(fixture.attendances.row(id).autoAbsentAt).isEqualTo(times.absentStart)
    }

    @Test
    fun `자동 결석은 잠근 뒤 다시 읽은 세션으로 연장된 마감과 삭제를 확인한다`() {
        val extendedId = fixture.addAttendance(session, memberId = 1L)
        // 스케줄러가 후보를 고른 뒤 운영진이 마감을 연장한 상황
        fixture.sessionCommandService.updateSession(updateCommandFor(session, times.lateStart, times.absentStart.plusSeconds(600)))

        assertThat(fixture.attendanceCommandService.closeExpiredAttendances(sessionId, times.absentStart)).isZero()
        assertThat(fixture.attendances.row(extendedId).status).isEqualTo(AttendanceStatus.PENDING)

        val deleted = fixture.createSession(cohortId, times)
        val deletedId = fixture.addAttendance(deleted, memberId = 1L)
        fixture.sessionCommandService.softDeleteSession(deleted.id!!)

        assertThat(fixture.attendanceCommandService.closeExpiredAttendances(deleted.id!!, times.absentStart)).isZero()
        assertThat(fixture.attendances.row(deletedId).status).isEqualTo(AttendanceStatus.PENDING)
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
