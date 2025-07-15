package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceiptPhoto.infrastructure.entity.ReceiptPhotoEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "receipts")
class ReceiptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "split_amount", nullable = false)
    val splitAmount: Int,
    @Column(name = "amount", nullable = false)
    val amount: Int,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val receiptPhotos: MutableList<ReceiptPhotoEntity> = mutableListOf(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    val gathering: GatheringEntity,
) {
    companion object {
        fun from(receipt: Receipt): ReceiptEntity {
            return ReceiptEntity(
                id = receipt.id?.value ?: 0L,
                splitAmount = receipt.splitAmount,
                amount = receipt.amount,
                createdAt = receipt.createdAt ?: Instant.now(),
                updatedAt = receipt.updatedAt ?: Instant.now(),
                deletedAt = receipt.deletedAt,
                receiptPhotos = receipt.receiptPhotos.map { ReceiptPhotoEntity.from(it) }.toMutableList(),
                gathering = GatheringEntity.from(receipt.gathering),
            )
        }
    }

    fun toDomain(): Receipt {
        return Receipt(
            id = com.server.dpmcore.gathering.gatheringReceipt.domain.model.ReceiptId(id),
            splitAmount = splitAmount,
            amount = amount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            receiptPhotos = receiptPhotos.map { it.toDomain() }.toMutableList(),
            gathering = gathering.toDomain(),
        )
    }
}
