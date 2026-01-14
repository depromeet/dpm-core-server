package core.application.bill.presentation.controller

import core.application.bill.application.service.BillCommandService
import core.application.bill.presentation.request.CreateBillRequest
import core.application.bill.presentation.request.UpdateGatheringJoinsRequest
import core.application.bill.presentation.request.UpdateMemberDepositRequest
import core.application.bill.presentation.request.UpdateMemberListDepositRequest
import core.application.bill.presentation.response.BillPersistenceResponse
import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.GatheringQueryService
import core.application.security.annotation.CurrentMemberId
import core.domain.bill.vo.BillId
import core.domain.member.vo.MemberId
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasAuthority('create:bill')")
    @PostMapping
    override fun createBill(
        @CurrentMemberId hostUserId: MemberId,
        @Valid @RequestBody createBillRequest: CreateBillRequest,
    ): CustomResponse<BillPersistenceResponse> {
        val billId = billCommandService.saveBillWithGatherings(hostUserId, createBillRequest)
        return CustomResponse.created(BillPersistenceResponse(billId))
    }

    @PreAuthorize("hasAuthority('create:bill')")
    @PatchMapping("/{billId}/close-participation")
    override fun closeBillParticipation(
        @PathVariable("billId") billId: Long,
    ): CustomResponse<BillPersistenceResponse> {
        val updatedBillId = billCommandService.closeBillParticipation(BillId(billId))
        return CustomResponse.ok(BillPersistenceResponse(updatedBillId))
    }

    @PreAuthorize("hasAuthority('read:bill')")
    @PatchMapping("/{billId}/check")
    override fun markBillAsChecked(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        billCommandService.markBillAsChecked(billId, memberId)
        return CustomResponse.noContent()
    }

    @PreAuthorize("hasAuthority('create:bill')")
    @PatchMapping("/{billId}/participation-confirm")
    override fun submitBillParticipationConfirm(
        @Positive @PathVariable billId: BillId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        billCommandService.submitBillParticipationConfirm(
            billId,
            memberId,
        )
        return CustomResponse.noContent()
    }

    @PreAuthorize("hasAuthority('read:bill')")
    @PatchMapping("/{billId}/join")
    override fun markAsGatheringJoined(
        @Positive @PathVariable billId: BillId,
        @RequestBody request: UpdateGatheringJoinsRequest,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        billCommandService.markAsJoinedEachGathering(billId, request, memberId)
        return CustomResponse.noContent()
    }

    @PreAuthorize("hasAuthority('update:bill')")
    @PatchMapping("/{billId}/members/{memberId}/deposit")
    override fun updateMemberDeposit(
        @Positive @PathVariable billId: BillId,
        @PathVariable memberId: MemberId,
        @RequestBody @Valid updateMemberDepositRequest: UpdateMemberDepositRequest,
    ): CustomResponse<Void> {
        billCommandService.updateMemberDeposit(updateMemberDepositRequest.toCommand(billId, memberId))
        return CustomResponse.noContent()
    }

    @PreAuthorize("hasAuthority('update:bill')")
    @PatchMapping("/{billId}/members/deposit")
    override fun updateMemberListDeposit(
        @Positive @PathVariable billId: BillId,
        @RequestBody @Valid updateMemberListDepositRequest: UpdateMemberListDepositRequest,
    ): CustomResponse<Void> {
        billCommandService.updateMemberListDeposit(updateMemberListDepositRequest.toCommand(billId))
        return CustomResponse.noContent()
    }
}
