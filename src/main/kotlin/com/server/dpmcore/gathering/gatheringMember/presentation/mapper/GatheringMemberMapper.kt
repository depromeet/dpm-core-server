package com.server.dpmcore.gathering.gatheringMember.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailGatheringMemberResponse
import com.server.dpmcore.gathering.gatheringMember.application.exception.GatheringMemberIdRequiredException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember

object GatheringMemberMapper {
//    fun toGatheringMember(gatheringMemberForBillCreateRequest: GatheringMemberForBillCreateRequest): GatheringMember =
//        GatheringMember(
//            memberId = MemberId(gatheringMemberForBillCreateRequest.memberId),
//            isChecked = gatheringMemberForBillCreateRequest.isCompleted,
//            isJoined = gatheringMemberForBillCreateRequest.isJoined,
//        )

    fun toCreateGatheringMemberResponse(gatheringMember: GatheringMember): BillDetailGatheringMemberResponse =
        BillDetailGatheringMemberResponse(
            gatheringMemberId =
                gatheringMember.id
                    ?: throw GatheringMemberIdRequiredException(),
            memberId = gatheringMember.memberId.value,
            isCompleted = gatheringMember.isViewed,
            isJoined = gatheringMember.isJoined,
        )
}
