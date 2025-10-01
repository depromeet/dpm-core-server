package core.application.bill.presentation.controller

import core.application.bill.application.service.BillQueryService
import core.application.bill.presentation.response.BillDetailResponse
import core.application.bill.presentation.response.BillListResponse
import core.application.bill.presentation.response.BillMemberSubmittedListResponse
import core.application.bill.presentation.response.BillSummaryListByMemberResponse
import core.application.bill.presentation.response.SubmittedParticipantEachGatheringResponse
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.bill.vo.BillId
import core.domain.member.vo.MemberId
import jakarta.validation.constraints.Positive
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/bills")
class BillQueryController(
    private val billQueryService: BillQueryService,
) : BillQueryApi {
    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{billId}")
    override fun getBillDetails(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<BillDetailResponse> {
        val response = billQueryService.getBillDetails(billId, memberId)
        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping
    override fun getBillListByMemberId(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<BillListResponse> {
//        TODO : 초대된 정산서만 조회하는 로직 추가(참여하지 않더라도)
//        TODO : 참여하는 정산서만 조회하는 로직 추가
        val response = billQueryService.getBillByMemberId(memberId)
        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{billId}/members")
    override fun getMemberBillSummaries(
        @Positive @PathVariable billId: BillId,
    ): CustomResponse<BillSummaryListByMemberResponse> {
        val response = billQueryService.getMemberBillSummaries(billId)
        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{billId}/members/submitted-participant")
    override fun getSubmittedParticipantEachGathering(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<SubmittedParticipantEachGatheringResponse> {
        val response = billQueryService.getSubmittedParticipantEachGathering(billId, memberId)
        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/{billId}/members/submitted-members")
    override fun getBillMemberSubmittedList(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<BillMemberSubmittedListResponse> {
        val response = billQueryService.getBillMemberSubmittedList(billId)
        return CustomResponse.ok(BillMemberSubmittedListResponse(response))
    }
}
