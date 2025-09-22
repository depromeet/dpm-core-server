package core.application.bill.presentation.response

import core.domain.gathering.vo.GatheringId
import io.swagger.v3.oas.annotations.media.Schema

data class SubmittedGatheringParticipationResponse(
    @field:Schema(
        description = "회식 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val gatheringId: GatheringId,
    @field:Schema(
        description = "회식 참여 여부 (참여: true, 불참: false, 미응답: null)",
        example = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val isJoined: Boolean?,
) {
    companion object {
        fun of(
            gatheringId: GatheringId,
            isJoined: Boolean?,
        ): SubmittedGatheringParticipationResponse =
            SubmittedGatheringParticipationResponse(
                gatheringId = gatheringId,
                isJoined = isJoined,
            )
    }
}
