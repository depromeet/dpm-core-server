package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.application.exception.BillNotFoundException
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillSummaryListByMemberResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.SubmittedGatheringParticipationResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.SubmittedParticipantEachGatheringResponse
import com.server.dpmcore.bill.bill.presentation.mapper.BillMapper
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.member.member.domain.model.MemberId
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
    override fun getBillByMemberId(memberId: MemberId): BillListResponse = getAllBills(memberId)

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
