package core.application.bill.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

class CreateReceiptResponse(
    @field:Schema(
        description = "회식 영수증 금액",
        example = "1732400",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val amount: Int,
)
