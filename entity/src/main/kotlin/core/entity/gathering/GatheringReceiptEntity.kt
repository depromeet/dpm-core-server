package core.entity.gathering

import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
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
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    val gathering: GatheringEntity? = null,
) {
    companion object {
        fun from(
            gatheringReceipt: GatheringReceipt,
            gathering: Gathering,
        ): GatheringReceiptEntity =
            GatheringReceiptEntity(
                id = gatheringReceipt.id?.value ?: 0L,
                splitAmount = gatheringReceipt.splitAmount,
                amount = gatheringReceipt.amount,
                createdAt = gatheringReceipt.createdAt ?: Instant.now(),
                updatedAt = gatheringReceipt.updatedAt ?: Instant.now(),
                deletedAt = gatheringReceipt.deletedAt,
                gathering = GatheringEntity.from(gathering),
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
            gatheringId = GatheringId(gathering?.id ?: 0L),
        )
}
