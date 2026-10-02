package core.application.image.application.properties

import org.springframework.boot.context.properties.ConfigurationProperties

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
