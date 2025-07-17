package com.server.dpmcore.gathering.gatheringMember.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.CreateGatheringMemberRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateGatheringMemberResponse
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.member.member.domain.model.MemberId

object GatheringMemberMapper {
    fun toGatheringMember(createGatheringMemberRequest: CreateGatheringMemberRequest): GatheringMember =
        GatheringMember(
            memberId = MemberId(createGatheringMemberRequest.memberId),
            isChecked = createGatheringMemberRequest.isCompleted,
            isJoined = createGatheringMemberRequest.isJoined,
        )

    fun toCreateGatheringMemberResponse(gatheringMember: GatheringMember): CreateGatheringMemberResponse =
        CreateGatheringMemberResponse(
            memberId = gatheringMember.memberId.value,
            isCompleted = gatheringMember.isChecked,
            isJoined = gatheringMember.isJoined,
        )
}
