package com.server.dpmcore.gathering.gathering.domain.port.outbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.gathering.gathering.domain.model.Gathering

interface GatheringPersistencePort {
    fun findGatheringById(id: Long): Gathering

    fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering

    fun findById(id: Long): Gathering

    fun findByBillId(billId: Long): List<Gathering>

    fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    )
}
