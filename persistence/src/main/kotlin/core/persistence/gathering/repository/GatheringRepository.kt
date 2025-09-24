package core.persistence.gathering.repository

import core.domain.bill.aggregate.Bill
import core.domain.bill.vo.BillId
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.port.outbound.GatheringPersistencePort
import core.domain.gathering.port.outbound.query.SubmittedParticipantGathering
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId
import core.entity.gathering.GatheringEntity
import jooq.dsl.tables.references.GATHERINGS
import jooq.dsl.tables.references.GATHERING_MEMBERS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GatheringRepository(
    private val gatheringJpaRepository: GatheringJpaRepository,
    private val dsl: DSLContext,
) : GatheringPersistencePort {
    override fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering = gatheringJpaRepository.save(GatheringEntity.from(bill, gathering)).toDomain()

    override fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    ) {
        gatheringJpaRepository
            .saveAll(
                gatherings.map { GatheringEntity.from(bill, it) },
            ).map { it.toDomain() }
    }

    override fun findByBillId(billId: BillId): List<Gathering> =
        gatheringJpaRepository.findByBillId(billId.value).map {
            it.toDomain()
        }

    override fun findAllGatheringIdsByBillId(billId: BillId): List<GatheringId> =
        dsl.select(GATHERINGS.GATHERING_ID)
            .from(GATHERINGS)
            .where(GATHERINGS.BILL_ID.eq(billId.value))
            .fetch(GATHERINGS.GATHERING_ID)
            .mapNotNull { it?.let { GatheringId(it) } }

    override fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering> =
        dsl.select(
            GATHERINGS.GATHERING_ID,
            GATHERING_MEMBERS.IS_JOINED
        )
            .from(GATHERINGS)
            .join(GATHERING_MEMBERS)
            .on(GATHERINGS.GATHERING_ID.eq(GATHERING_MEMBERS.GATHERING_ID))
            .where(
                GATHERINGS.BILL_ID.eq(billId.value)
                    .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value))
            )
            .fetch()
            .mapNotNull { record ->
                val gatheringId = record[GATHERINGS.GATHERING_ID]
                val isJoined = record[GATHERING_MEMBERS.IS_JOINED]

                if (gatheringId != null && isJoined != null) {
                    SubmittedParticipantGathering(
                        gatheringId = GatheringId(gatheringId),
                        isJoined = isJoined
                    )
                } else null
            }
}
