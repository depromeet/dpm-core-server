package core.it.image

import core.persistence.image.repository.ImageRepository
import core.persistence.image.repository.ImageUploadRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/** 이미지 업로드 MySQL 통합 테스트 전용 최소 컨텍스트. core.application 밖 패키지라 전체 컴포넌트 스캔을 타지 않는다. */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["core.entity"])
@EnableJpaRepositories(basePackages = ["core.persistence.image"])
@Import(ImageUploadRepository::class, ImageRepository::class)
class ImageUploadMySqlIntegrationTestApplication
