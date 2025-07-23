package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.application.BillQueryService
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateBillResponse
import com.server.dpmcore.common.exception.CustomResponse
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/bills")
class BillQueryController(
    private val billQueryService: BillQueryService,
) : BillQueryApi {
    @GetMapping("/{billId}")
    override fun getBillDetails(
        @Positive @PathVariable billId: BillId,
    ): CustomResponse<CreateBillResponse> {
        val response = billQueryService.getBillDetails(billId)
        return CustomResponse.ok(response)
    }
}
