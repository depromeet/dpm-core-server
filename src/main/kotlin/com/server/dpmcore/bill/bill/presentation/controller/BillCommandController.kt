package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.application.BillCommandService
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateBillResponse
import com.server.dpmcore.bill.bill.presentation.mapper.BillMapper.toCreateBillResponse
import com.server.dpmcore.common.exception.CustomResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/bills")
class BillCommandController(
    private val billCommandService: BillCommandService,
//    TODO : 도메인끼리 의존성을 어떻게 하면 좋을지 논의해보고 싶음.
//    매퍼에서 port를 사용하게 되면 매퍼도 bean이되고, 물흐르듯 정방향으로 가던게 갑자기 역방향이 돼서 맘에 안듦.
) : BillCommandApi {
    @PostMapping
    override fun createBill(
        @RequestBody createBillRequest: CreateBillRequest,
    ): CustomResponse<CreateBillResponse> {
        val bill = billCommandService.save(createBillRequest)

        return CustomResponse.created(toCreateBillResponse(bill))
    }
}
