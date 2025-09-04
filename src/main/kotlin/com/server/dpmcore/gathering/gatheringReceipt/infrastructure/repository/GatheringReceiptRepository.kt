package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.application.exception.GatheringReceiptNotFoundException
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.inbound.command.GatheringReceiptSplitAmountCommand
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.outbound.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.jooq.DSLContext
import org.jooq.generated.tables.references.GATHERING_RECEIPTS
import org.springframework.data.repository.findByIdOrNull
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

    override fun findById(gatheringReceiptId: GatheringReceiptId): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByIdOrNull(
            gatheringReceiptId.value,
        ) ?: throw GatheringReceiptNotFoundException()

    override fun findByGathering(gatheringId: GatheringId): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByGatheringId(gatheringId)
            ?: throw GatheringReceiptNotFoundException()

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringReceipt는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * 애그리거트 루트에서 시작해 하위 도메인까지 전달되는 업데이트 명령을 Command 객체로 캡슐화하여, 데이터 무결성과 의도를 명확히 보장하고자 함
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateSplitAmountById(command: GatheringReceiptSplitAmountCommand): Int {
        dsl
            .update(GATHERING_RECEIPTS)
            .set(GATHERING_RECEIPTS.SPLIT_AMOUNT, command.splitAmount)
            .set(GATHERING_RECEIPTS.UPDATED_AT, LocalDateTime.now())
            .where(GATHERING_RECEIPTS.GATHERING_RECEIPT_ID.eq(command.gatheringReceiptId))
            .execute()

        return command.splitAmount ?: 0
    }

    override fun findSplitAmountByGatheringId(gatheringId: GatheringId): Int? =
        dsl
            .select(GATHERING_RECEIPTS.SPLIT_AMOUNT)
            .from(GATHERING_RECEIPTS)
            .where(GATHERING_RECEIPTS.GATHERING_ID.eq(gatheringId.value))
            .fetchOne()
            ?.get(GATHERING_RECEIPTS.SPLIT_AMOUNT)
}
