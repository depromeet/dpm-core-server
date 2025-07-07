package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.gathering.gatheringReceiptPhoto.infrastructure.entity.ReceiptPhotoEntity
import jakarta.persistence.*
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

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Int,

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
)
