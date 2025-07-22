package com.server.dpmcore.gathering.gathering.domain.port.outbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

interface GatheringPersistencePort {
    fun findGatheringById(id: Long): Gathering

    fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering

    fun findById(id: Long): Gathering

    fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    )

    fun findAllByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering>
}
