package core.entity.image

import core.domain.image.aggregate.Image
import core.domain.image.enums.ImageContentType
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "images",
    uniqueConstraints = [UniqueConstraint(name = "uk_images_object_key", columnNames = ["object_key"])],
    indexes = [Index(name = "idx_images_owner_member_id", columnList = "owner_member_id")],
)
class ImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "owner_member_id", nullable = false, updatable = false)
    val ownerMemberId: Long,
    @Column(name = "object_key", nullable = false, updatable = false, length = 100)
    val objectKey: String,
    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    val contentType: String,
    @Column(name = "size_bytes", nullable = false, updatable = false)
    val size: Long,
    @Column(name = "original_file_name", updatable = false, length = 255)
    val originalFileName: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
) {
    fun toDomain(): Image =
        Image(
            id = ImageId(id),
            ownerMemberId = MemberId(ownerMemberId),
            objectKey = objectKey,
            contentType = ImageContentType.fromMimeType(contentType),
            size = size,
            originalFileName = originalFileName,
            createdAt = createdAt,
        )

    companion object {
        fun from(image: Image): ImageEntity =
            ImageEntity(
                id = image.id?.value ?: 0L,
                ownerMemberId = image.ownerMemberId.value,
                objectKey = image.objectKey,
                contentType = image.contentType.mimeType,
                size = image.size,
                originalFileName = image.originalFileName,
                createdAt = image.createdAt,
            )
    }
}
