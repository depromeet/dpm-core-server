package com.server.dpmcore.bill.bill.presentation.mapper

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateBillResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateGatheringResponse
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BillMapper(
    private val gatheringQueryUseCase: GatheringQueryUseCase,
) {
    fun toBillCreateResponse(bill: Bill): CreateBillResponse {
        val gatherings = gatheringQueryUseCase.getAllGatheringsByGatheringIds(bill.gatheringIds)

        return CreateBillResponse(
            title = bill.title,
            description = bill.description,
            hostUserId = bill.hostUserId.value,
            billTotalAmount = 100,
            createdAt =
                LocalDateTime.ofInstant(
                    bill.createdAt,
                    ZoneId.of(TIME_ZONE),
                ),
            billAccountId = bill.billAccount.id?.value ?: 0L,
            gatherings = gatherings.map { CreateGatheringResponse.from(it) },
        )
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}
