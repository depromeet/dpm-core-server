package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import java.time.LocalDateTime

data class BillDetailGatheringResponse(
    val title: String,
    val description: String?,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: String,
    val joinMemberCount: Int,
    val amount: Int,
    val gatheringMembers: List<BillDetailGatheringMemberResponse>,
) {
    companion object {
        fun from(
            gathering: Gathering,
            gatheringMembers: List<GatheringMember>,
        ): BillDetailGatheringResponse =
            BillDetailGatheringResponse(
                title = gathering.title,
                description = gathering.description,
                roundNumber = gathering.roundNumber,
                heldAt =
                    LocalDateTime.ofInstant(
                        gathering.heldAt,
                        java.time.ZoneId.of(TIME_ZONE),
                    ),
                category = gathering.category.name,
                joinMemberCount = gathering.getGatheringJoinMemberCount(),
                amount = gathering.gatheringReceipt?.amount ?: 0,
                gatheringMembers =
                    gatheringMembers.map {
                        BillDetailGatheringMemberResponse.from(
                            it,
                        )
                    },
            )

        private const val TIME_ZONE = "Asia/Seoul"
    }
}
