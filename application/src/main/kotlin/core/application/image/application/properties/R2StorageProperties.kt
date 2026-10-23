package core.application.image.application.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Cloudflare R2(S3 호환) 설정. `image.provider=r2` 일 때만 쓴다. 값이 비어 있어도 기동은 되고 이미지 API 첫 호출에서 503 이 된다.
 * URL 수명·lease 같은 공통 값은 [ImageStorageProperties] 를 그대로 쓴다.
 */
@ConfigurationProperties(prefix = "image.r2")
data class R2StorageProperties(
    val accountId: String = "",
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val bucket: String = "",
    // 비우면 https://{accountId}.r2.cloudflarestorage.com. 로컬 S3 호환 저장소(MinIO 등)를 쓸 때만 지정한다.
    val endpoint: String = "",
    val connectTimeoutMillis: Int = 3_000,
    val readTimeoutMillis: Int = 15_000,
) {
    fun resolvedEndpoint(): String = endpoint.ifBlank { "https://$accountId.r2.cloudflarestorage.com" }

    fun missingSettings(): List<String> =
        buildList {
            if (accountId.isBlank() && endpoint.isBlank()) add("account-id")
            if (accessKeyId.isBlank()) add("access-key-id")
            if (secretAccessKey.isBlank()) add("secret-access-key")
            if (bucket.isBlank()) add("bucket")
        }
}
