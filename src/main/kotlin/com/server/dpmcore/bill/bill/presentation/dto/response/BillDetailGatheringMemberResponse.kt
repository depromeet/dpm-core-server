package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId
import io.swagger.v3.oas.annotations.media.Schema

data class BillDetailGatheringMemberResponse(
    @field:Schema(
        description = "'회식 참여 멤버' 일련번호(gatheringMember의 pk 값입니다)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val gatheringMemberId: GatheringMemberId,
    @field:Schema(
        description = "멤버 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberId: Long,
    // TODO : 추후 이름 추가
//    val name: String? = null,
    @field:Schema(
        description = "회식 참여 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isJoined: Boolean?,
    @field:Schema(
        description = "정산 완료 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
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
