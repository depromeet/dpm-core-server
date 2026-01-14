package core.application.bill.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

data class GatheringMemberForBillCreateRequest(
    @field:Schema(
        description = "멤버 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberId: Long,
    @field:Schema(
        description = "회식 참여 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isJoined: Boolean = true,
    @field:Schema(
        description = "정산 완료 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isCompleted: Boolean = false,
)
