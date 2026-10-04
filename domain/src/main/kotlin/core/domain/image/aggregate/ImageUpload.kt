package core.domain.image.aggregate

import core.domain.image.enums.ImageContentType
import core.domain.image.enums.ImageUploadStatus
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * 프론트가 OCI 로 직접 올리는 업로드 한 건. 이미지 행은 검증과 확정 키 복사가 끝난 뒤에만 만든다.
 *
 * 객체 키는 저장하지 않고 [id] 로 정한다. 업로드 대상은 [stagingKey](쓰기 PAR 이 걸리는 유일한 키),
 * 확정 이미지는 [finalKey] 다. finalKey 에는 쓰기 PAR 을 만들지 않고 서버의 복사(원본 ETag 일치 + 대상 if-none-match)로만 쓰므로,
 * finalKey 에 객체가 있으면 검증한 바이트 그대로다. images.object_key UNIQUE 가 한 세션에서 이미지가 둘 생기는 것을 막는다.
 *
 * [parId] 는 쓰기 PAR 식별자(URL 아님)다. COMPLETED 에서는 PAR 회수와 업로드 객체 삭제가 모두 끝나면 null 로 비워 정리 대상에서 뺀다.
 */
class ImageUpload(
    val id: String,
    val ownerMemberId: MemberId,
    val contentType: ImageContentType,
    val size: Long,
    val parId: String?,
    val status: ImageUploadStatus,
    val expiresAt: Instant,
    val leaseToken: String? = null,
    val leaseUntil: Instant? = null,
    val etag: String? = null,
    val workRequestId: String? = null,
    val imageId: ImageId? = null,
    val failureCode: String? = null,
    val createdAt: Instant,
) {
    val stagingKey: String get() = STAGING_KEY_PREFIX + id

    val finalKey: String get() = Image.OBJECT_KEY_PREFIX + id

    fun isOwnedBy(memberId: MemberId): Boolean = ownerMemberId == memberId

    fun isLeaseFree(now: Instant): Boolean = leaseToken == null || leaseUntil == null || !leaseUntil.isAfter(now)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageUpload) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    // parId, lease token 은 남기지 않는다.
    override fun toString(): String =
        "ImageUpload(id=$id, ownerMemberId=$ownerMemberId, contentType=$contentType, size=$size, " +
            "status=$status, imageId=$imageId)"

    companion object {
        const val STAGING_KEY_PREFIX = "uploads/"

        fun create(
            id: String,
            ownerMemberId: MemberId,
            contentType: ImageContentType,
            size: Long,
            parId: String,
            expiresAt: Instant,
            createdAt: Instant,
        ): ImageUpload =
            ImageUpload(
                id = id,
                ownerMemberId = ownerMemberId,
                contentType = contentType,
                size = size,
                parId = parId,
                status = ImageUploadStatus.PENDING,
                expiresAt = expiresAt,
                createdAt = createdAt,
            )
    }
}
