package core.application.image.infrastructure

import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.properties.ImageStorageProperties.AuthMode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LazyOciObjectStorageClientProviderTest {
    @Test
    fun `설정이 비어 있으면 인증을 시도하지 않고 503`() {
        val provider = LazyOciObjectStorageClientProvider(ImageStorageProperties())

        assertThat(ImageStorageProperties().missingSettings()).containsExactly("region", "namespace", "bucket")
        assertThatThrownBy { provider.get() }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `config-file 모드에서 파일이 없으면 503 이며 다음 호출도 다시 시도한다`() {
        val provider =
            LazyOciObjectStorageClientProvider(
                ImageStorageProperties(
                    authMode = AuthMode.CONFIG_FILE,
                    region = "ap-seoul-1",
                    namespace = "test-ns",
                    bucket = "test-bucket",
                    configFilePath = "/nonexistent/dpm-oci-config",
                ),
            )

        repeat(2) {
            assertThatThrownBy { provider.get() }.isInstanceOf(ImageStorageUnavailableException::class.java)
        }
    }

    @Test
    fun `만든 적 없는 클라이언트는 종료 시 만들지 않는다`() {
        assertThatCode { LazyOciObjectStorageClientProvider(ImageStorageProperties()).destroy() }
            .doesNotThrowAnyException()
    }
}
