package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId
import com.server.dpmcore.gathering.gatheringReceiptPhoto.infrastructure.entity.GatheringReceiptPhotoEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "gathering_receipts")
class GatheringReceiptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "split_amount")
    val splitAmount: Int? = null,
    @Column(name = "amount", nullable = false)
    val amount: Int,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val receiptPhotos: MutableList<GatheringReceiptPhotoEntity>? = mutableListOf(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    val gathering: GatheringEntity? = null,
) {
    companion object {
        fun from(gatheringReceipt: GatheringReceipt): GatheringReceiptEntity =
            GatheringReceiptEntity(
                id = gatheringReceipt.id?.value ?: 0L,
                splitAmount = gatheringReceipt.splitAmount,
                amount = gatheringReceipt.amount,
                createdAt = gatheringReceipt.createdAt ?: Instant.now(),
                updatedAt = gatheringReceipt.updatedAt ?: Instant.now(),
                deletedAt = gatheringReceipt.deletedAt,
                receiptPhotos =
                    gatheringReceipt.gatheringReceiptPhotos
                        ?.map {
                            GatheringReceiptPhotoEntity.from(
                                it,
                            )
                        }?.toMutableList(),
                gathering = gatheringReceipt.gathering?.let { GatheringEntity.from(it) },
            )
    }

    fun toDomain(): GatheringReceipt =
        GatheringReceipt(
            id = GatheringReceiptId(id),
            splitAmount = splitAmount,
            amount = amount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            gatheringReceiptPhotos = receiptPhotos?.map { it.toDomain() }?.toMutableList(),
            gathering = gathering?.toDomain(),
        )
}
