package core.application.image.infrastructure

import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.properties.R2StorageProperties
import core.domain.image.port.outbound.ImageStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock

class ImageStorageProviderSelectionTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(StorageConfig::class.java)

    @Test
    fun `provider 가 없으면 기존 OCI 구현을 쓴다`() {
        runner.run { context ->
            assertThat(context.getBean(ImageStoragePort::class.java)).isInstanceOf(OciImageStorageAdapter::class.java)
            assertThat(context).doesNotHaveBean(R2ImageStorageAdapter::class.java)
        }
    }

    @Test
    fun `provider 가 r2 면 R2 구현이 우선한다`() {
        runner.withPropertyValues("image.provider=r2").run { context ->
            assertThat(context.getBean(ImageStoragePort::class.java)).isInstanceOf(R2ImageStorageAdapter::class.java)
        }
    }

    @Configuration
    @EnableConfigurationProperties(ImageStorageProperties::class, R2StorageProperties::class)
    @Import(OciImageStorageAdapter::class, LazyOciObjectStorageClientProvider::class, R2ImageStorageAdapter::class, LazyR2ClientProvider::class)
    class StorageConfig {
        @Bean
        fun clock(): Clock = Clock.systemUTC()
    }
}
