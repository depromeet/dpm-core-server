package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import com.server.dpmcore.gathering.gatheringReceiptPhoto.infrastructure.entity.ReceiptPhotoEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "receipts")
class ReceiptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id", nullable = false, updatable = false)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    val gathering: GatheringEntity,

    @Column(name = "split_amount", nullable = false)
    val splitAmount: Int,

    @Column(name = "amount", nullable = false)
    val amount: Int,

    @Column(name = "bill_paper_url", nullable = false)
    val billPaperUrl: String,

    @Column(name = "is_completed", nullable = false)
    val isCompleted: Boolean = false,

    @OneToMany(mappedBy = "receipt", cascade = [CascadeType.ALL], orphanRemoval = true)
    val receiptPhoto: MutableList<ReceiptPhotoEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime,

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null
)
