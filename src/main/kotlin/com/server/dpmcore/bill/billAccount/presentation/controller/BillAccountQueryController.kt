package com.server.dpmcore.bill.billAccount.presentation.controller

import com.server.dpmcore.bill.billAccount.application.BillAccountQueryService
import com.server.dpmcore.bill.billAccount.presentation.dto.response.BillAccountResponse
import com.server.dpmcore.bill.billAccount.presentation.mapper.BillAccountMapper.toBillAccountResponse
import com.server.dpmcore.common.exception.CustomResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/accounts")
class BillAccountQueryController(
    private val billAccountQueryService: BillAccountQueryService,
) : BillAccountQueryApi{
    @GetMapping("/{billAccountId}")
    override fun getBillAccount(
        @PathVariable("billAccountId")
        billAccountId: Long,
        ): CustomResponse<BillAccountResponse> {
        val billAccount = billAccountQueryService.findBy(billAccountId)
        return CustomResponse.ok(
            toBillAccountResponse(billAccount)
        )
    }
}
