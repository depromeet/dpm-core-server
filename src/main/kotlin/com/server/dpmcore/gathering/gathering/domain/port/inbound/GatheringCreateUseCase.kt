package com.server.dpmcore.gathering.gathering.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand

interface GatheringCreateUseCase {
    fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        billId: BillId,
    )
}
