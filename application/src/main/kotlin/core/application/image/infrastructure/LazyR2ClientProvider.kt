package core.application.image.infrastructure

import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.R2StorageProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

/**
 * 첫 이미지 요청에서 R2 클라이언트를 만든다. 설정이 비어 있으면 캐시하지 않고 503 을 던져 다음 요청이 다시 시도한다.
 */
@Component
@ConditionalOnProperty(prefix = "image", name = ["provider"], havingValue = "r2")
class LazyR2ClientProvider(
    private val properties: R2StorageProperties,
) : R2ClientProvider,
    DisposableBean {
    private val logger = KotlinLogging.logger { }

    @Volatile
    private var clients: Pair<S3Client, S3Presigner>? = null

    override fun client(): S3Client = get().first

    override fun presigner(): S3Presigner = get().second

    override fun destroy() {
        synchronized(this) {
            val created = clients ?: return
            clients = null
            runCatching { created.first.close() }.onFailure { logger.warn(it) { "R2 클라이언트를 닫지 못했습니다" } }
            runCatching { created.second.close() }.onFailure { logger.warn(it) { "R2 presigner 를 닫지 못했습니다" } }
        }
    }

    private fun get(): Pair<S3Client, S3Presigner> =
        clients ?: synchronized(this) {
            clients ?: create().also { clients = it }
        }

    private fun create(): Pair<S3Client, S3Presigner> {
        val missing = properties.missingSettings()
        if (missing.isNotEmpty()) {
            logger.error { "R2 설정이 비어 있습니다: image.r2.$missing" }
            throw ImageStorageUnavailableException()
        }
        val endpoint = URI.create(properties.resolvedEndpoint())
        val credentials =
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId, properties.secretAccessKey),
            )
        // R2 는 버킷을 경로로 받는 방식도 지원한다. 계정 엔드포인트 하나로 인증서·DNS 를 단순하게 둔다.
        val s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build()
        val client =
            S3Client
                .builder()
                .endpointOverride(endpoint)
                .region(R2_REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Configuration)
                // SDK 2.30+ 의 기본 CRC 체크섬 헤더를 R2 가 모두 받지는 않으므로 필요할 때만 보낸다.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(
                    UrlConnectionHttpClient
                        .builder()
                        .connectionTimeout(Duration.ofMillis(properties.connectTimeoutMillis.toLong()))
                        .socketTimeout(Duration.ofMillis(properties.readTimeoutMillis.toLong())),
                )
                // 사용자 요청 안에서 호출하므로 재시도하지 않고 바로 503 으로 돌려준다.
                .overrideConfiguration(
                    ClientOverrideConfiguration.builder().retryStrategy(AwsRetryStrategy.doNotRetry()).build(),
                )
                .build()
        val presigner =
            S3Presigner
                .builder()
                .endpointOverride(endpoint)
                .region(R2_REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Configuration)
                .build()
        return client to presigner
    }

    companion object {
        // R2 는 리전이 하나뿐이며 서명에는 auto 를 쓴다.
        private val R2_REGION = Region.of("auto")
    }
}
