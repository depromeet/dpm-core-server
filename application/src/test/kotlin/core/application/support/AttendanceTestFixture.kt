package core.application.support

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.attendance.application.service.AttendanceCommandService
import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.attendance.application.service.AttendanceQueryService
import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.MemberQueryService
import core.application.session.application.service.SessionCommandService
import core.application.session.application.service.SessionQueryService
import core.application.session.application.validator.SessionValidator
import core.domain.attendance.enums.AttendanceStatus
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.session.aggregate.Session
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionAttendanceTimes
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Collections

/** 가짜 저장소 위에 실제 서비스를 조립한다. 트랜잭션/잠금은 동작하지 않는다(DB 잠금과 동시성은 MySQL 통합 테스트에서 검증). */
class AttendanceTestFixture(
    now: Instant,
    val policyProperties: AttendancePolicyProperties = AttendancePolicyProperties(),
) {
    val clock = MutableClock(now)
    val attendances = FakeAttendancePersistencePort()
    val sessions = FakeSessionPersistencePort()
    val cohorts = FakeCohortPersistencePort()
    val notifications = RecordingSentSessionNotificationCommandUseCase()
    val events: MutableList<Any> = Collections.synchronizedList(mutableListOf())
    val memberQueryUseCase: MemberQueryUseCase = mock(MemberQueryUseCase::class.java)

    val sessionValidator = SessionValidator()
    val cohortQueryService = CohortQueryService(cohorts)

    val attendanceQueryService =
        AttendanceQueryService(
            memberQueryService = mock(MemberQueryService::class.java),
            attendancePersistencePort = attendances,
            attendanceGraduationEvaluator = mock(AttendanceGraduationEvaluator::class.java),
        )

    val sessionQueryService =
        SessionQueryService(
            cohortQueryUseCase = cohortQueryService,
            sessionPersistencePort = sessions,
            attendanceQueryService = attendanceQueryService,
            memberQueryUseCase = memberQueryUseCase,
            clock = clock,
        )

    val attendanceCommandService =
        AttendanceCommandService(
            attendancePersistencePort = attendances,
            sessionPersistencePort = sessions,
            memberQueryUseCase = memberQueryUseCase,
            sessionValidator = sessionValidator,
            clock = clock,
        )

    val sessionCommandService =
        SessionCommandService(
            sessionPersistencePort = sessions,
            eventPublisher = ApplicationEventPublisher { events += it },
            sessionValidator = sessionValidator,
            cohortQueryService = cohortQueryService,
            sentSessionNotificationCommandUseCase = notifications,
            attendancePolicyProperties = policyProperties,
            attendanceCommandService = attendanceCommandService,
            clock = clock,
        )

    fun createActiveCohort(value: String = "18"): CohortId {
        val cohortId = cohorts.save(Cohort(value = value)).id!!
        cohorts.activate(cohortId)
        return cohortId
    }

    fun createSession(
        cohortId: CohortId,
        times: SessionAttendanceTimes,
        code: String = CODE,
    ): Session =
        sessions.save(
            Session(
                cohortId = cohortId,
                date = times.attendanceStart,
                week = 1,
                place = "온라인",
                eventName = "1주차 세션",
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = times.attendanceStart,
                        lateStart = times.lateStart,
                        absentStart = times.absentStart,
                        attendanceCode = code,
                    ),
            ),
        )

    fun addAttendance(
        session: Session,
        memberId: Long,
        status: AttendanceStatus = AttendanceStatus.PENDING,
        attendedAt: Instant? = null,
        updatedAt: Instant? = null,
    ): Long =
        attendances.insert(
            sessionId = session.id!!.value,
            memberId = memberId,
            status = status,
            attendedAt = attendedAt,
            updatedAt = updatedAt,
        )

    companion object {
        const val CODE = "1234"
    }
}
