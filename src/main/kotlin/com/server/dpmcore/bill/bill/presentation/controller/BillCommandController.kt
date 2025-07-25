package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.application.BillCommandService
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.BillPersistenceResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.application.GatheringQueryService
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.security.annotation.CurrentMemberId
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/bills")
class BillCommandController(
    private val billCommandService: BillCommandService,
    private val gatheringQueryService: GatheringQueryService,
//    TODO : 도메인끼리 의존성을 어떻게 하면 좋을지 논의해보고 싶음.
//    매퍼에서 port를 사용하게 되면 매퍼도 bean이되고, 물흐르듯 정방향으로 가던게 갑자기 역방향이 돼서 맘에 안듦.
) : BillCommandApi {
    @PostMapping
    override fun createBill(
        @CurrentMemberId hostUserId: MemberId,
        @Valid @RequestBody createBillRequest: CreateBillRequest,
    ): CustomResponse<BillPersistenceResponse> {
        val billId = billCommandService.saveBillWithGatherings(hostUserId, createBillRequest)
        return CustomResponse.created(BillPersistenceResponse(billId))
    }

    @PatchMapping("/{billId}/close-participation")
    override fun closeBillParticipation(
        @PathVariable("billId") billId: Long,
    ): CustomResponse<BillPersistenceResponse> {
        val updatedBillId = billCommandService.closeBillParticipation(BillId(billId))
        return CustomResponse.ok(BillPersistenceResponse(updatedBillId))
    }

    @PostMapping("/{billId}/confirmed")
    override fun markBillAsChecked(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        billCommandService.markBillAsChecked(billId, memberId)
        return CustomResponse.noContent()
    }
}
