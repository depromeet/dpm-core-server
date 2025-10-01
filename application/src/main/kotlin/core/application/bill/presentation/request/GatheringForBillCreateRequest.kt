package core.application.bill.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class GatheringForBillCreateRequest(
    @field:Schema(
        description = "회식 제목",
        example = "OT 1차 회식",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val title: String,
    @field:Schema(
        description = "회식 설명",
        example = "부대찌개집에서 진행됨",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val description: String?,
    @field:Schema(
        description = "회식 차수(1차, 2차 등)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val roundNumber: Int,
    @field:Schema(
        description = "회식이 열린 일시",
        example = "2025-08-01T16:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val heldAt: LocalDateTime,
    val receipt: ReceiptForBillCreateRequest,
)
