package core.persistence.gathering.repository

import com.server.dpmcore.bill.bill.domain.model.Bill
import core.domain.bill.vo.BillId
import com.server.dpmcore.gathering.gathering.application.exception.GatheringIdRequiredException
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotFoundException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import core.domain.gathering.vo.GatheringId
import com.server.dpmcore.gathering.gathering.domain.model.query.SubmittedParticipantGathering
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import core.entity.gathering.GatheringEntity
import com.server.dpmcore.gathering.gatheringMember.application.exception.GatheringMemberNotFoundException
import org.jooq.DSLContext
import org.jooq.generated.tables.references.GATHERINGS
import org.jooq.generated.tables.references.GATHERING_MEMBERS
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

    override fun findById(id: Long): Gathering =
        gatheringJpaRepository
            .findById(id)
            .orElseThrow { GatheringNotFoundException() }
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
            .map { GatheringId(it ?: throw GatheringIdRequiredException()) }

    override fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering> =
        dsl
            .select(
                GATHERINGS.GATHERING_ID,
                GATHERING_MEMBERS.IS_JOINED,
            ).from(GATHERINGS)
            .join(GATHERING_MEMBERS)
            .on(GATHERINGS.GATHERING_ID.eq(GATHERING_MEMBERS.GATHERING_ID))
            .where(
                GATHERINGS.BILL_ID
                    .eq(billId.value)
                    .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
            ).fetch { record ->
                SubmittedParticipantGathering(
                    gatheringId =
                        GatheringId(
                            record[GATHERINGS.GATHERING_ID] ?: throw GatheringNotFoundException(),
                        ),
                    isJoined =
                        record[GATHERING_MEMBERS.IS_JOINED]
                            ?: throw GatheringMemberNotFoundException(),
                )
            }
}
