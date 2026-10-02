package core.domain.image.aggregate

import core.domain.image.enums.ImageContentType
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import java.time.Instant
import java.util.UUID

/**
 * 업로드된 이미지의 메타데이터. 원본 바이트는 비공개 오브젝트 스토리지의 [objectKey] 에 있다.
 * objectKey 는 서버가 만든 UUID 로만 정하며 외부로 노출하지 않는다.
 */
class Image(
    val id: ImageId? = null,
    val ownerMemberId: MemberId,
    val objectKey: String,
    val contentType: ImageContentType,
    val size: Long,
    val createdAt: Instant,
) {
    fun isOwnedBy(memberId: MemberId): Boolean = ownerMemberId == memberId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Image) return false
        return id == other.id && objectKey == other.objectKey
    }

    override fun hashCode(): Int = 31 * (id?.hashCode() ?: 0) + objectKey.hashCode()

    override fun toString(): String =
        "Image(id=$id, ownerMemberId=$ownerMemberId, contentType=$contentType, size=$size)"

    companion object {
        private const val OBJECT_KEY_PREFIX = "images/"

        fun create(
            ownerMemberId: MemberId,
            contentType: ImageContentType,
            size: Long,
            createdAt: Instant,
        ): Image =
            Image(
                ownerMemberId = ownerMemberId,
                objectKey = OBJECT_KEY_PREFIX + UUID.randomUUID(),
                contentType = contentType,
                size = size,
                createdAt = createdAt,
            )
    }
}
