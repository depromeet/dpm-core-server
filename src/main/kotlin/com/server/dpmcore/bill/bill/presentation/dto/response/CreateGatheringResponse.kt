package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import java.time.LocalDateTime

data class CreateGatheringResponse(
    val title: String,
    val description: String?,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: String,
    val joinMemberCount: Int,
    val amount: Int,
    val gatheringMembers: List<CreateGatheringMemberResponse>,
) {
    companion object {
        fun from(gathering: Gathering): CreateGatheringResponse =
            CreateGatheringResponse(
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
                gatheringMembers = gathering.gatheringMembers.map { CreateGatheringMemberResponse.from(it) },
            )

        private const val TIME_ZONE = "Asia/Seoul"
    }
}
