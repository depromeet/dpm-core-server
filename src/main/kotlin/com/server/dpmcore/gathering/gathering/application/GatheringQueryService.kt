package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberQueryService
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.application.GatheringReceiptQueryService
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringQueryService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val gatheringReceiptQueryService: GatheringReceiptQueryService,
    private val gatheringMemberQueryService: GatheringMemberQueryService,
) : GatheringQueryUseCase {
    fun findById(gatheringId: Long) =
        gatheringPersistencePort
            .findById(gatheringId)

    override fun getAllGatheringsByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering> =
        gatheringPersistencePort.findAllByGatheringIds(gatheringIds)

    override fun findByBillId(billId: BillId): List<Gathering> =
        gatheringPersistencePort
            .findByBillId(billId)

    override fun findGatheringReceiptByGatheringId(gatheringId: GatheringId): GatheringReceipt =
        gatheringReceiptQueryService
            .findByGatheringId(gatheringId)

    override fun findGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringMemberQueryService
            .getGatheringMemberByGatheringId(gatheringId)
}
