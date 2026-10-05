package core.application.image.application.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * OCI Object Storage 설정. 값이 비어 있어도 기동은 되고, 이미지 API 첫 호출에서 503 으로 드러난다.
 * 기본 인증은 Instance Principal 이며 로컬에서는 CONFIG_FILE 로 ~/.oci/config 프로필을 쓴다.
 */
@ConfigurationProperties(prefix = "image.storage")
data class ImageStorageProperties(
    val authMode: AuthMode = AuthMode.INSTANCE_PRINCIPAL,
    val region: String = "",
    val namespace: String = "",
    val bucket: String = "",
    val configFilePath: String = "",
    val configProfile: String = "DEFAULT",
    val connectTimeoutMillis: Int = 3_000,
    val readTimeoutMillis: Int = 15_000,
    // Instance Principal 이 metadata 서비스(169.254.169.254)를 찾을 때. SDK 기본값은 8회·최대 30초 백오프다.
    val metadataTimeoutMillis: Int = 2_000,
    val metadataRetries: Int = 1,
    // Instance Principal 이 auth 서비스에서 토큰을 받을 때. 사용자 요청 안에서 발급될 수 있어 Object Storage 보다 짧게 둔다.
    val authConnectTimeoutMillis: Int = 2_000,
    val authReadTimeoutMillis: Int = 3_000,
    // 프론트 직접 업로드용 쓰기 PAR 수명. 만료 뒤에는 새 검증을 시작하지 않는다.
    val uploadUrlTtl: Duration = Duration.ofMinutes(10),
    // 조회용 읽기 PAR 수명. URL 을 가진 사람은 만료 전까지 누구나 읽을 수 있다.
    val readUrlTtl: Duration = Duration.ofMinutes(1),
    // 검증·복사 처리 lease. 내려받기(읽기 타임아웃 15초)와 디코드보다 넉넉해야 한다.
    val processingLease: Duration = Duration.ofMinutes(2),
    // 이 시간이 지난 미완료 세션과 정리가 덜 된 세션을 정리한다.
    val abandonedAfter: Duration = Duration.ofHours(24),
    val cleanupBatchSize: Int = 50,
) {
    enum class AuthMode {
        INSTANCE_PRINCIPAL,
        CONFIG_FILE,
    }

    fun missingSettings(): List<String> =
        buildList {
            if (region.isBlank()) add("region")
            if (namespace.isBlank()) add("namespace")
            if (bucket.isBlank()) add("bucket")
            if (authMode == AuthMode.CONFIG_FILE && configFilePath.isBlank()) add("config-file-path")
        }
}
