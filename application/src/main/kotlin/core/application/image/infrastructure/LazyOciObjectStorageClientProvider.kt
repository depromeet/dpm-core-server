package core.application.image.infrastructure

import com.oracle.bmc.ClientConfiguration
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.http.ClientConfigurator
import com.oracle.bmc.http.client.StandardClientProperties
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.retrier.RetryConfiguration
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.properties.ImageStorageProperties.AuthMode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 첫 이미지 요청에서 OCI 클라이언트를 만든다. 기동 시에는 metadata 서비스나 로컬 자격 증명에 접근하지 않는다.
 * 생성에 실패하면 캐시하지 않으므로 다음 요청이 다시 시도한다(시도당 metadata 탐지는 retries·timeout 으로 제한).
 */
@Component
class LazyOciObjectStorageClientProvider(
    private val properties: ImageStorageProperties,
) : ObjectStorageClientProvider,
    DisposableBean {
    private val logger = KotlinLogging.logger { }

    @Volatile
    private var client: ObjectStorage? = null

    override fun get(): ObjectStorage = client ?: synchronized(this) { client ?: create().also { client = it } }

    /** 만들어진 적이 있을 때만 닫는다. 종료를 위해 클라이언트를 새로 만들지 않는다. */
    override fun destroy() {
        synchronized(this) {
            val created = client ?: return
            client = null
            try {
                created.close()
            } catch (e: Exception) {
                logger.warn(e) { "OCI Object Storage 클라이언트를 닫지 못했습니다" }
            }
        }
    }

    private fun create(): ObjectStorage {
        val missing = properties.missingSettings()
        if (missing.isNotEmpty()) {
            logger.error { "OCI Object Storage 설정이 비어 있습니다: image.storage.$missing" }
            throw ImageStorageUnavailableException()
        }
        return try {
            ObjectStorageClient
                .builder()
                .region(properties.region)
                .configuration(
                    ClientConfiguration
                        .builder()
                        .connectionTimeoutMillis(properties.connectTimeoutMillis)
                        .readTimeoutMillis(properties.readTimeoutMillis)
                        // 사용자 요청 안에서 호출하므로 SDK 재시도(최대 8회·30초)를 끄고 바로 503 으로 돌려준다.
                        .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
                        .build(),
                ).isStreamWarningEnabled(false)
                .build(authenticationProvider())
        } catch (e: Exception) {
            logger.error(e) { "OCI Object Storage 클라이언트를 만들지 못했습니다: authMode=${properties.authMode}" }
            throw ImageStorageUnavailableException()
        }
    }

    private fun authenticationProvider(): BasicAuthenticationDetailsProvider =
        when (properties.authMode) {
            AuthMode.INSTANCE_PRINCIPAL ->
                InstancePrincipalsAuthenticationDetailsProvider
                    .builder()
                    .detectEndpointRetries(properties.metadataRetries)
                    .timeoutForEachRetry(properties.metadataTimeoutMillis)
                    // SDK 기본값으로는 metadata 의 region·인증서·키 조회와 auth 서비스 토큰 발급에 read timeout 이 없다
                    // (인증서·키만 OCI_JAVASDK_CERTIFICATE_URL_CONNECTION_* 환경 변수로 지정 가능). 시도마다 적용되며 SDK 고정 재시도는 남는다.
                    .federationClientMetadataConfigurator(
                        timeouts(properties.metadataTimeoutMillis, properties.metadataTimeoutMillis),
                    ).federationClientConfigurator(
                        timeouts(properties.connectTimeoutMillis, properties.readTimeoutMillis),
                    ).build()
            AuthMode.CONFIG_FILE ->
                ConfigFileAuthenticationDetailsProvider(properties.configFilePath, properties.configProfile)
        }

    private fun timeouts(
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int,
    ) = ClientConfigurator { builder ->
        builder
            .property(StandardClientProperties.CONNECT_TIMEOUT, Duration.ofMillis(connectTimeoutMillis.toLong()))
            .property(StandardClientProperties.READ_TIMEOUT, Duration.ofMillis(readTimeoutMillis.toLong()))
    }
}
