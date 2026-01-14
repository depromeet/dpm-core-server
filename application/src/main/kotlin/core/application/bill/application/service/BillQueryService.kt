package core.application.bill.application.service

import core.application.bill.application.exception.BillNotFoundException
import core.application.bill.presentation.mapper.BillMapper
import core.application.bill.presentation.response.BillDetailResponse
import core.application.bill.presentation.response.BillListResponse
import core.application.bill.presentation.response.BillSummaryListByMemberResponse
import core.application.bill.presentation.response.SubmittedGatheringParticipationResponse
import core.application.bill.presentation.response.SubmittedParticipantEachGatheringResponse
import core.domain.bill.aggregate.Bill
import core.domain.bill.port.inbound.BillQueryUseCase
import core.domain.bill.port.outbound.BillPersistencePort
import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel
import core.domain.bill.vo.BillId
import core.domain.gathering.port.inbound.GatheringQueryUseCase
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class BillQueryService(
    private val billPersistencePort: BillPersistencePort,
    private val billMapper: BillMapper,
    private val gatheringQueryUseCase: GatheringQueryUseCase,
) : BillQueryUseCase {
    override fun getById(billId: BillId): Bill =
        billPersistencePort.findById(billId)
            ?: throw BillNotFoundException()

    fun getBillDetails(
        billId: BillId,
        memberId: MemberId,
    ): BillDetailResponse = billMapper.toBillDetailResponse(getById(billId), memberId)

    // TODO: 현재 17기 멤버만 존재하여 getAllBills()로 처리. 향후 다른 기수 추가 시 memberId 기반 필터링 구현 필요
    fun getBillByMemberId(memberId: MemberId): BillListResponse = getAllBills(memberId)

    fun getAllBills(memberId: MemberId): BillListResponse {
        val bills = billPersistencePort.findAllBills()
        return billMapper.toBillListResponse(bills, memberId)
    }

    fun getMemberBillSummaries(billId: BillId): BillSummaryListByMemberResponse {
        val queryModels =
            gatheringQueryUseCase.getGatheringMemberReceiptByBillId(billId)

        val responses =
            queryModels.map { model ->
                BillSummaryListByMemberResponse.BillSummaryByMemberResponse.of(
                    name = model.memberName,
                    authority = model.memberAuthority,
                    splitAmount = model.memberSplitAmount,
                )
            }

        return BillSummaryListByMemberResponse(responses)
    }

    fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): SubmittedParticipantEachGatheringResponse {
        val queryModels =
            gatheringQueryUseCase.getSubmittedParticipantEachGathering(billId, memberId)
        val responses =
            queryModels.map { model ->
                SubmittedGatheringParticipationResponse.of(
                    gatheringId = model.gatheringId,
                    isJoined = model.isJoined,
                )
            }
        return SubmittedParticipantEachGatheringResponse(responses)
    }

    fun getBillMemberSubmittedList(billId: BillId): List<BillMemberIsInvitationSubmittedQueryModel> =
        gatheringQueryUseCase.getBillMemberSubmittedList(billId)
}
