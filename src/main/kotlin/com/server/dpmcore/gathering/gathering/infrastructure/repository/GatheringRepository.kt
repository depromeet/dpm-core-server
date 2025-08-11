package com.server.dpmcore.gathering.gathering.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.listQuery
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gathering.application.query.model.SubmittedParticipantGathering
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import org.jooq.DSLContext
import org.jooq.generated.tables.references.GATHERINGS
import org.jooq.generated.tables.references.GATHERING_MEMBERS
import org.springframework.stereotype.Repository

@Repository
class GatheringRepository(
    private val gatheringJpaRepository: GatheringJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
) : GatheringPersistencePort {
    override fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering = gatheringJpaRepository.save(GatheringEntity.from(bill, gathering)).toDomain()

    override fun findGatheringById(id: Long) =
        queryFactory
            .singleQuery<GatheringEntity> {
                select(entity(GatheringEntity::class))
                from(entity(GatheringEntity::class))
//                where(GatheringEntity::id id)
            }.toDomain()

    override fun findById(id: Long): Gathering =
        gatheringJpaRepository
            .findById(id)
            .orElseThrow { GatheringException.GatheringNotFoundException() }
            .toDomain()

    override fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    ) {
        gatheringJpaRepository
            .saveAll(
                gatherings.map { GatheringEntity.from(bill, it) },
            ).map { it.toDomain() }
    }

    override fun findAllByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering> =
        gatheringIds
            .map { it.value }
            .let { ids ->
                queryFactory
                    .listQuery<GatheringEntity> {
                        select(entity(GatheringEntity::class))
                        from(entity(GatheringEntity::class))
                        where(col(GatheringEntity::id).`in`(ids))
                    }.map { it.toDomain() }
            }

    override fun findByBillId(billId: BillId): List<Gathering> =
        gatheringJpaRepository.findByBillId(billId.value).map {
            it.toDomain()
        }

    override fun findAllGatheringIdsByBillId(billId: BillId): List<GatheringId> =
        dsl
            .select(GATHERINGS.GATHERING_ID)
            .from(GATHERINGS)
            .where(GATHERINGS.BILL_ID.eq(billId.value))
            .fetch(GATHERINGS.GATHERING_ID)
            .map { GatheringId(it ?: throw GatheringException.GatheringIdRequiredException()) }

    override fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering> {
        return dsl
            .select(
                GATHERINGS.GATHERING_ID,
                GATHERING_MEMBERS.IS_JOINED,
            )
            .from(GATHERINGS)
            .join(GATHERING_MEMBERS)
            .on(GATHERINGS.GATHERING_ID.eq(GATHERING_MEMBERS.GATHERING_ID))
            .where(
                GATHERINGS.BILL_ID.eq(billId.value)
                    .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
            )
            .fetch { record ->
                SubmittedParticipantGathering(
                    gatheringId =
                        GatheringId(
                            record[GATHERINGS.GATHERING_ID] ?: throw GatheringException.GatheringNotFoundException(),
                        ),
                    isJoined =
                        record[GATHERING_MEMBERS.IS_JOINED]
                            ?: throw GatheringMemberException.GatheringMemberNotFoundException(),
                )
            }
    }
}
