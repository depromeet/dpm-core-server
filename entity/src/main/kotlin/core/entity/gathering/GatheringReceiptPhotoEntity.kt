package core.entity.gathering

import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceiptPhoto
import core.domain.gathering.vo.GatheringReceiptPhotoId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "gathering_receipt_photos")
class GatheringReceiptPhotoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_photo_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "url", nullable = false)
    val url: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    val receipt: GatheringReceiptEntity,
) {
    companion object {
        fun from(
            gatheringReceiptPhoto: GatheringReceiptPhoto,
            gathering: Gathering,
        ): GatheringReceiptPhotoEntity =
            GatheringReceiptPhotoEntity(
                id = gatheringReceiptPhoto.id?.value ?: 0L,
//                TODO : url이 없는 경우 체크 필요
                url = gatheringReceiptPhoto.url!!,
                createdAt = gatheringReceiptPhoto.createdAt ?: Instant.now(),
                updatedAt = gatheringReceiptPhoto.updatedAt ?: Instant.now(),
                deletedAt = gatheringReceiptPhoto.deletedAt,
//                TODO : receipt가 없는 경우 체크 필요
                receipt = GatheringReceiptEntity.from(gatheringReceiptPhoto.gatheringReceipt!!, gathering),
            )
    }

    fun toDomain(): GatheringReceiptPhoto =
        GatheringReceiptPhoto(
            id = GatheringReceiptPhotoId(id),
            url = url,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            gatheringReceipt = receipt.toDomain(),
        )
}
