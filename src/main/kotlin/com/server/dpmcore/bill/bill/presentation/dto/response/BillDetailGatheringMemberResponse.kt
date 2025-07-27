package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId

data class BillDetailGatheringMemberResponse(
    val gatheringMemberId: GatheringMemberId,
    val memberId: Long,
    // TODO : 추후 이름 추가
//    val name: String? = null,
    val isJoined: Boolean,
    val isCompleted: Boolean,
) {
    companion object {
        fun from(gatheringMember: GatheringMember): BillDetailGatheringMemberResponse =
            BillDetailGatheringMemberResponse(
                gatheringMemberId =
                    gatheringMember.id
                        ?: throw GatheringMemberException.GatheringMemberIdRequiredException(),
                memberId = gatheringMember.memberId.value,
//                name = gatheringMember.memberId.name,
                isJoined = gatheringMember.isJoined,
                isCompleted = gatheringMember.isConfirmed(),
            )
    }
}
