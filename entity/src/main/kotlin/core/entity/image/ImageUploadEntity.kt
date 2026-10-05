package core.entity.image

import core.domain.image.aggregate.ImageUpload
import core.domain.image.enums.ImageContentType
import core.domain.image.enums.ImageUploadStatus
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/** 상태 변경은 ImageUploadJpaRepository 의 조건부 UPDATE 로만 한다. */
@Entity
@Table(
    name = "image_uploads",
    uniqueConstraints = [UniqueConstraint(name = "uk_image_uploads_image_id", columnNames = ["image_id"])],
    indexes = [
        Index(name = "idx_image_uploads_owner_member_id", columnList = "owner_member_id"),
        Index(name = "idx_image_uploads_created_at", columnList = "created_at"),
    ],
)
class ImageUploadEntity(
    @Id
    @Column(name = "upload_id", nullable = false, updatable = false, length = 36)
    val id: String,
    @Column(name = "owner_member_id", nullable = false, updatable = false)
    val ownerMemberId: Long,
    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    val contentType: String,
    @Column(name = "size_bytes", nullable = false, updatable = false)
    val size: Long,
    @Column(name = "par_id", length = 255)
    val parId: String?,
    @Column(name = "status", nullable = false, length = 20)
    val status: String,
    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,
    @Column(name = "lease_token", length = 36)
    val leaseToken: String?,
    @Column(name = "lease_until")
    val leaseUntil: Instant?,
    @Column(name = "etag", length = 255)
    val etag: String?,
    @Column(name = "work_request_id", length = 255)
    val workRequestId: String?,
    @Column(name = "image_id")
    val imageId: Long?,
    @Column(name = "failure_code", length = 50)
    val failureCode: String?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
) {
    fun toDomain(): ImageUpload =
        ImageUpload(
            id = id,
            ownerMemberId = MemberId(ownerMemberId),
            contentType = ImageContentType.fromMimeType(contentType),
            size = size,
            parId = parId,
            status = ImageUploadStatus.valueOf(status),
            expiresAt = expiresAt,
            leaseToken = leaseToken,
            leaseUntil = leaseUntil,
            etag = etag,
            workRequestId = workRequestId,
            imageId = imageId?.let(::ImageId),
            failureCode = failureCode,
            createdAt = createdAt,
        )

    companion object {
        fun from(upload: ImageUpload): ImageUploadEntity =
            ImageUploadEntity(
                id = upload.id,
                ownerMemberId = upload.ownerMemberId.value,
                contentType = upload.contentType.mimeType,
                size = upload.size,
                parId = upload.parId,
                status = upload.status.name,
                expiresAt = upload.expiresAt,
                leaseToken = upload.leaseToken,
                leaseUntil = upload.leaseUntil,
                etag = upload.etag,
                workRequestId = upload.workRequestId,
                imageId = upload.imageId?.value,
                failureCode = upload.failureCode,
                createdAt = upload.createdAt,
            )
    }
}
