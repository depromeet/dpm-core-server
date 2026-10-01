package core.application.attendance.application.service

import core.application.support.AttendanceTestFixture
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.aggregate.Cohort
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class AttendanceAutoAbsenceServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()

    @Test
    fun `서버 중단으로 놓친 마감을 재시작 후 한 번에 모두 처리한다`() {
        val sessions =
            (1..3).map { day ->
                val start = now.plus(Duration.ofDays(day.toLong()))
                fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(start))
            }
        val ids = sessions.map { fixture.addAttendance(it, memberId = 1L) }

        // 마지막 세션 마감 후 일주일 뒤 재시작
        fixture.clock.now = sessions.last().attendancePolicy.absentStart.plus(Duration.ofDays(7))
        val restarted =
            AttendanceAutoAbsenceService(
                sessionPersistencePort = fixture.sessions,
                attendanceCommandService = fixture.attendanceCommandService,
                clock = fixture.clock,
            )

        assertThat(restarted.closeExpiredAttendances()).isEqualTo(3)
        ids.forEach { assertThat(fixture.attendances.row(it).status).isEqualTo(AttendanceStatus.ABSENT) }
        assertThat(restarted.closeExpiredAttendances()).isZero()
    }

    @Test
    fun `반복 실행해도 추가로 바뀌는 기록이 없다`() {
        val session = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(3600)))
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.clock.now = session.attendancePolicy.absentStart

        val first = fixture.autoAbsenceService.closeExpiredAttendances()
        val second = fixture.autoAbsenceService.closeExpiredAttendances()
        val third = fixture.autoAbsenceService.closeExpiredAttendances()

        assertThat(first).isEqualTo(1)
        assertThat(second).isZero()
        assertThat(third).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(id).autoAbsentAt).isEqualTo(session.attendancePolicy.absentStart)
    }

    @Test
    fun `과거 기수의 오래 전 마감 세션도 미인증이면 자동 결석하고 운영진 결정과 인증 기록은 그대로 둔다`() {
        val oldCohortId = fixture.cohorts.save(Cohort(value = "15")).id!!
        val historic =
            fixture.createSession(oldCohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minus(Duration.ofDays(700))))
        val pending = fixture.addAttendance(historic, memberId = 1L)
        val present =
            fixture.addAttendance(historic, memberId = 2L, status = AttendanceStatus.PRESENT, attendedAt = historic.attendancePolicy.attendanceStart)
        val late =
            fixture.addAttendance(historic, memberId = 3L, status = AttendanceStatus.LATE, attendedAt = historic.attendancePolicy.lateStart)
        val excused = fixture.addAttendance(historic, memberId = 4L, status = AttendanceStatus.EXCUSED_ABSENT, updatedAt = now)
        val manualPending = fixture.addAttendance(historic, memberId = 5L, updatedAt = now)
        val manualPresent = fixture.addAttendance(historic, memberId = 6L, status = AttendanceStatus.PRESENT, updatedAt = now)
        val legacyAbsent = fixture.addAttendance(historic, memberId = 7L, status = AttendanceStatus.ABSENT)

        val closed = fixture.autoAbsenceService.closeExpiredAttendances()

        assertThat(closed).isEqualTo(1)
        assertThat(fixture.attendances.row(pending).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(pending).autoAbsentAt).isEqualTo(now)
        assertThat(fixture.attendances.row(pending).updatedAt).isNull()
        assertThat(fixture.attendances.row(present).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(late).status).isEqualTo(AttendanceStatus.LATE)
        assertThat(fixture.attendances.row(excused).status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(fixture.attendances.row(manualPending).status).isEqualTo(AttendanceStatus.PENDING)
        assertThat(fixture.attendances.row(manualPresent).status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(fixture.attendances.row(legacyAbsent).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(legacyAbsent).autoAbsentAt).isNull()
    }

    @Test
    fun `아직 마감 전인 세션은 처리하지 않는다`() {
        val session = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plusSeconds(3600)))
        val id = fixture.addAttendance(session, memberId = 1L)
        fixture.clock.now = session.attendancePolicy.absentStart.minusNanos(1)

        assertThat(fixture.autoAbsenceService.closeExpiredAttendances()).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
    }

    @Test
    fun `모든 기수의 마감 지난 세션을 처리한다`() {
        val otherCohortId = fixture.cohorts.save(Cohort(value = "17")).id!!
        val current = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plus(Duration.ofDays(1))))
        val other = fixture.createSession(otherCohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.plus(Duration.ofDays(2))))
        val otherHistoric =
            fixture.createSession(otherCohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minus(Duration.ofDays(200))))
        val currentId = fixture.addAttendance(current, memberId = 1L)
        val otherId = fixture.addAttendance(other, memberId = 2L)
        val historicId = fixture.addAttendance(otherHistoric, memberId = 2L)

        fixture.clock.now = now.plus(Duration.ofDays(3))
        val closed = fixture.autoAbsenceService.closeExpiredAttendances()

        assertThat(closed).isEqualTo(3)
        assertThat(fixture.attendances.row(currentId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(otherId).status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(fixture.attendances.row(historicId).status).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `삭제된 과거 세션은 처리하지 않는다`() {
        val historic = fixture.createSession(cohortId, AttendanceTimeOffsets.DEFAULT.resolveFor(now.minus(Duration.ofDays(30))))
        val id = fixture.addAttendance(historic, memberId = 1L)
        fixture.sessionCommandService.softDeleteSession(historic.id!!)

        assertThat(fixture.autoAbsenceService.closeExpiredAttendances()).isZero()
        assertThat(fixture.attendances.row(id).status).isEqualTo(AttendanceStatus.PENDING)
    }

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

    /** 실제 AttendanceCommandService 와 같은 의존성으로 만든 하위 클래스 (kotlin-spring 으로 open 됨) */
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
