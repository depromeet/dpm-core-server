package core.persistence.gathering.repository.receipt

import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.outbound.GatheringReceiptPersistencePort
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId
import core.entity.gathering.GatheringReceiptEntity
import org.jooq.dsl.tables.references.GATHERING_RECEIPTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GatheringReceiptRepository(
    private val gatheringReceiptJpaRepository: GatheringReceiptJpaRepository,
    private val dsl: DSLContext,
) : GatheringReceiptPersistencePort {
    override fun save(
        gatheringReceipt: GatheringReceipt,
        gathering: Gathering,
    ) {
        gatheringReceiptJpaRepository.save(GatheringReceiptEntity.from(gatheringReceipt, gathering))
    }

    override fun findById(gatheringReceiptId: GatheringReceiptId): GatheringReceipt? =
        gatheringReceiptJpaRepository.findById(gatheringReceiptId)?.toDomain()

    override fun findByGathering(gatheringId: GatheringId): GatheringReceipt? =
        gatheringReceiptJpaRepository.findByGatheringId(gatheringId)?.toDomain()

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringReceipt는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateSplitAmountById(gatheringReceipt: GatheringReceipt): Int? {
        dsl
            .update(GATHERING_RECEIPTS)
            .set(GATHERING_RECEIPTS.SPLIT_AMOUNT, gatheringReceipt.splitAmount)
            .set(GATHERING_RECEIPTS.UPDATED_AT, LocalDateTime.now())
            .where(GATHERING_RECEIPTS.RECEIPT_ID.eq(gatheringReceipt.id?.value))
            .execute()

        return gatheringReceipt.splitAmount
    }

    override fun findSplitAmountByGatheringId(gatheringId: GatheringId): Int? =
        dsl
            .select(GATHERING_RECEIPTS.SPLIT_AMOUNT)
            .from(GATHERING_RECEIPTS)
            .where(GATHERING_RECEIPTS.GATHERING_ID.eq(gatheringId.value))
            .fetchOne()
            ?.get(GATHERING_RECEIPTS.SPLIT_AMOUNT)
}
