package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.gathering.exception.GatheringReceiptException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.jooq.DSLContext
import org.jooq.generated.tables.references.GATHERING_RECEIPTS
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class GatheringReceiptRepository(
    private val gatheringReceiptJpaRepository: GatheringReceiptJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
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
        ) ?: throw GatheringReceiptException.GatheringReceiptNotFoundException()

    override fun findByGathering(gatheringId: GatheringId): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByGatheringId(gatheringId)
            ?: throw GatheringReceiptException.GatheringReceiptNotFoundException()

    override fun updateSplitAmount(gatheringReceipt: GatheringReceipt): Int =
        queryFactory
            .updateQuery(GatheringReceiptEntity::class) {
                set(col(GatheringReceiptEntity::splitAmount), gatheringReceipt.splitAmount)
                set(col(GatheringReceiptEntity::updatedAt), gatheringReceipt.updatedAt)
                where(
                    col(
                        GatheringReceiptEntity::id,
                    ).equal(
                        gatheringReceipt.id?.value
                            ?: throw GatheringReceiptException.GatheringReceiptIdRequiredException(),
                    ),
                )
            }.executeUpdate()

    override fun findSplitAmountByGatheringId(gatheringId: GatheringId): Int? =
        dsl
            .select(GATHERING_RECEIPTS.SPLIT_AMOUNT)
            .from(GATHERING_RECEIPTS)
            .where(GATHERING_RECEIPTS.GATHERING_ID.eq(gatheringId.value))
            .fetchOne()
            ?.get(GATHERING_RECEIPTS.SPLIT_AMOUNT)
}
