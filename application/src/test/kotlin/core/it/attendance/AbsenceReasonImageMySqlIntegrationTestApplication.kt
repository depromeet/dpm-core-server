package core.it.attendance

import core.application.attendance.application.service.AbsenceReasonCommandService
import core.application.attendance.application.service.AbsenceReasonImageQueryService
import core.application.attendance.application.service.AbsenceReasonQueryService
import core.application.attendance.application.service.AttendanceCommandService
import core.application.common.configuration.JooqDslConfig
import core.application.image.FakeImageStoragePort
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.service.ImageQueryService
import core.application.session.application.validator.SessionValidator
import core.application.support.MutableClock
import core.persistence.absencereason.repository.AbsenceReasonImageRepository
import core.persistence.absencereason.repository.AbsenceReasonRepository
import core.persistence.attendance.repository.AttendanceRepository
import core.persistence.cohort.repository.CohortRepository
import core.persistence.image.repository.ImageRepository
import core.persistence.session.repository.SessionRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Instant

/** 결석 사유서 첨부 MySQL 통합 테스트 전용 최소 컨텍스트. 저장소(OCI)는 호출 기록용 fake 로 바꾼다. */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["core.entity"])
@EnableJpaRepositories(basePackages = ["core.persistence"])
@Import(
    JooqDslConfig::class,
    AttendanceRepository::class,
    SessionRepository::class,
    CohortRepository::class,
    AbsenceReasonRepository::class,
    AbsenceReasonImageRepository::class,
    ImageRepository::class,
    SessionValidator::class,
    AttendanceCommandService::class,
    AbsenceReasonCommandService::class,
    AbsenceReasonQueryService::class,
    AbsenceReasonImageQueryService::class,
    ImageQueryService::class,
)
class AbsenceReasonImageMySqlIntegrationTestApplication {
    @Bean
    fun clock(): MutableClock = MutableClock(Instant.parse("2026-10-01T03:00:00Z"))

    @Bean
    fun imageStoragePort(): FakeImageStoragePort = FakeImageStoragePort()

    @Bean
    fun imageStorageProperties(): ImageStorageProperties = ImageStorageProperties()
}
