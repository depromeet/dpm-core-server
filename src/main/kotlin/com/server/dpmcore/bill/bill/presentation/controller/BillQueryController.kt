package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.application.BillQueryService
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.common.exception.CustomResponse
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
    override fun findBill(
        @PathVariable("billId") billId: BillId,
    ): CustomResponse<Void> {
        val bill = billQueryService.findBillById(billId)
        println("Bill found: $bill")
        return CustomResponse.ok()
    }
}
