package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringCategory
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import java.time.LocalDateTime

data class BillDetailGatheringResponse(
    val gatheringId: GatheringId,
    val title: String,
    val description: String?,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: GatheringCategory,
    val joinMemberCount: Int,
    val amount: Int,
    val splitAmount: Int,
) {
    companion object {
        fun from(
            gathering: Gathering,
            gatheringMembers: List<GatheringMember>,
            splitAmount: Int,
        ): BillDetailGatheringResponse =
            BillDetailGatheringResponse(
                gatheringId = gathering.id ?: throw GatheringException.GatheringIdRequiredException(),
                title = gathering.title,
                description = gathering.description,
                roundNumber = gathering.roundNumber,
                heldAt =
                    LocalDateTime.ofInstant(
                        gathering.heldAt,
                        java.time.ZoneId.of(TIME_ZONE),
                    ),
                category = gathering.category,
                joinMemberCount = gathering.getGatheringJoinMemberCount(),
                amount = gathering.gatheringReceipt?.amount ?: 0,
                splitAmount = splitAmount,
            )

        private const val TIME_ZONE = "Asia/Seoul"
    }
}
