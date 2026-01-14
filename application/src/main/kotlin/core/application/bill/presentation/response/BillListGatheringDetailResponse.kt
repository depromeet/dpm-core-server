package core.application.bill.presentation.response

import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.vo.GatheringId
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class BillListGatheringDetailResponse(
    @field:Schema(
        description = "회식 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val gatheringId: GatheringId,
    @field:Schema(
        description = "회식 제목",
        example = "OT 1차 회식",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val title: String,
    @field:Schema(
        description = "회식 설명",
        example = "부대찌개 집에서 먹은 OT 1차 회식입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val description: String? = null,
    @field:Schema(
        description = "회식의 차수(1차, 2차 등)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val roundNumber: Int,
    @field:Schema(
        description = "회식이 열린 일시",
        example = "2025-08-03T12:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val heldAt: LocalDateTime,
    @field:Schema(
        description = "회식의 카테고리(지금은 회식만 있으므로 GATHERING으로 고정)",
        example = "GATHERING",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val category: GatheringCategory,
    @field:Schema(
        description = "회식 영수증(지금은 사용하지 않습니다.)",
        example = "null",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val receipt: BillListGatheringReceiptDetailResponse? = null,
    @field:Schema(
        description = "회식에 참여한 멤버 수",
        example = "32",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val joinMemberCount: Int,
    @field:Schema(
        description = "회식의 총 금액",
        example = "1732000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val amount: Int,
    @field:Schema(
        description = "조회하는 멤버가 지불해야 할 회식에 대한 분할된 금액",
        example = "28300",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val splitAmount: Int?,
)
