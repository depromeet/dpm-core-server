package core.application.bill.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

data class ReceiptForBillCreateRequest(
    @field:Schema(
        description = "결제 금액(영수증 금액)",
        example = "1372400",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val amount: Int,
//    val receiptPhotos: MutableList<ReceiptPhotoForBillCreateRequest>?,
)
