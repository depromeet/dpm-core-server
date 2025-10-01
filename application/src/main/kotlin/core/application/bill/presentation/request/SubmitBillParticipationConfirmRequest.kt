package core.application.bill.presentation.request

import core.domain.gathering.vo.GatheringId
import io.swagger.v3.oas.annotations.media.Schema

data class SubmitBillParticipationConfirmRequest(
    @field:Schema(
        description = "회식 일련번호 목록",
        example = "[1, 2, 3]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val gatheringIds: List<GatheringId>,
)
