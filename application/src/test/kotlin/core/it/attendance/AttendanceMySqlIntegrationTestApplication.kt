package core.it.attendance

import core.application.attendance.application.event.listener.SessionCreateEventListener
import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.attendance.application.service.AttendanceCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.common.configuration.JooqDslConfig
import core.application.session.application.service.SessionCommandService
import core.application.session.application.validator.SessionValidator
import core.application.support.MutableClock
import core.persistence.attendance.repository.AttendanceRepository
import core.persistence.cohort.repository.CohortRepository
import core.persistence.session.repository.SessionRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Instant

/**
 * MySQL 통합 테스트 전용 최소 컨텍스트.
 *
 * 운영과 같은 JPA 엔티티/저장소/jOOQ 설정(JooqDslConfig)과 출석 관련 서비스, 세션 생성 이벤트 리스너만 올린다.
 * 패키지가 core.application 밖에 있어 CoreApplication 의 전체 컴포넌트 스캔(보안/OAuth/스케줄러 등)을 타지 않는다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["core.entity"])
@EnableConfigurationProperties(AttendancePolicyProperties::class)
@EnableJpaRepositories(basePackages = ["core.persistence"])
@Import(
    JooqDslConfig::class,
    SessionCreateEventListener::class,
    AttendanceRepository::class,
    SessionRepository::class,
    CohortRepository::class,
    SessionValidator::class,
    CohortQueryService::class,
    AttendanceCommandService::class,
    SessionCommandService::class,
)
class AttendanceMySqlIntegrationTestApplication {
    @Bean
    fun clock(): MutableClock = MutableClock(Instant.parse("2026-10-01T03:00:00Z"))
}
