package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.jooq.DSLContext
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

    override fun findBy(gatheringReceiptId: Long): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByIdOrNull(
            gatheringReceiptId,
        ) ?: throw IllegalArgumentException("Receipt not found with id: $gatheringReceiptId")

    override fun findByGathering(gatheringId: GatheringId): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByGatheringId(gatheringId.value)

    override fun updateSplitAmount(gatheringReceipt: GatheringReceipt): Int =
        queryFactory
            .updateQuery(GatheringReceiptEntity::class) {
                set(col(GatheringReceiptEntity::splitAmount), gatheringReceipt.splitAmount)
                set(col(GatheringReceiptEntity::updatedAt), gatheringReceipt.updatedAt)
                where(
                    col(
                        GatheringReceiptEntity::id,
                    ).equal(gatheringReceipt.id?.value ?: throw BillException.GatheringReceiptIdRequiredException()),
                )
            }.executeUpdate()
}
