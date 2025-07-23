package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.application.BillQueryService
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.security.annotation.CurrentMemberId
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
    ): CustomResponse<BillDetailResponse> {
        val response = billQueryService.getBillDetails(billId)
        return CustomResponse.ok(response)
    }

    @GetMapping
    override fun getBillListByMemberId(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<BillListResponse> {
//        TODO : 초대된 정산서만 조회하는 로직 추가(참여하지 않더라도)
//        TODO : 참여하는 정산서만 조회하는 로직 추가
        val response = billQueryService.getBillByMemberId(memberId)
        return CustomResponse.ok(response)
    }
}
