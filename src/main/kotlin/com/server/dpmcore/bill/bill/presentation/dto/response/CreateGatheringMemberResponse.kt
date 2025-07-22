package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember

data class CreateGatheringMemberResponse(
    val memberId: Long,
    // TODO : 추후 이름 추가
//    val name: String? = null,
    val isJoined: Boolean,
    val isCompleted: Boolean,
) {
    companion object {
        fun from(gatheringMember: GatheringMember): CreateGatheringMemberResponse =
            CreateGatheringMemberResponse(
                memberId = gatheringMember.memberId.value,
//                name = gatheringMember.memberId.name,
                isJoined = gatheringMember.isJoined,
                isCompleted = gatheringMember.isConfirmed(),
            )
    }
}
