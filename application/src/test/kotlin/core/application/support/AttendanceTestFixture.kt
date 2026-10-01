package core.application.support

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.cohort.application.service.CohortQueryService
import core.application.session.application.service.SessionCommandService
import core.application.session.application.validator.SessionValidator
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Collections

/**
 * 가짜 저장소 위에 실제 서비스들을 조립한다. 스프링 프록시가 없으므로 트랜잭션/잠금은 동작하지 않으며,
 * 이 픽스처로는 서비스 규칙만 검증한다.
 */
class AttendanceTestFixture(
    now: Instant,
    /** 서버 설정(attendance.policy). 기본은 10/15/30분. */
    val policyProperties: AttendancePolicyProperties = AttendancePolicyProperties(),
) {
    val clock = MutableClock(now)
    val sessions = FakeSessionPersistencePort()
    val cohorts = FakeCohortPersistencePort()
    val notifications = RecordingSentSessionNotificationCommandUseCase()
    val events: MutableList<Any> = Collections.synchronizedList(mutableListOf())

    val sessionValidator = SessionValidator()
    val cohortQueryService = CohortQueryService(cohorts)

    val sessionCommandService =
        SessionCommandService(
            sessionPersistencePort = sessions,
            eventPublisher = ApplicationEventPublisher { events += it },
            sessionValidator = sessionValidator,
            cohortQueryService = cohortQueryService,
            sentSessionNotificationCommandUseCase = notifications,
            attendancePolicyProperties = policyProperties,
        )

    fun createActiveCohort(value: String = "18"): CohortId {
        val cohortId = cohorts.save(Cohort(value = value)).id!!
        cohorts.activate(cohortId)
        return cohortId
    }
}
