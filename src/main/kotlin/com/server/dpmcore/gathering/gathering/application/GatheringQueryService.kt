package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.infrastructure.repository.GatheringRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringQueryService(
    private val gatheringRepository: GatheringRepository,
) {
    fun findById(gatheringId: Long): Gathering =
        gatheringRepository
            .findById(gatheringId)

    fun findByBillId(billId: Long): List<Gathering> =
        gatheringRepository
            .findByBillId(billId)
}
