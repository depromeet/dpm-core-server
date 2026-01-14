package core.application.bill.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

data class ReceiptPhotoForBillCreateRequest(
    @field:Schema(
        description = "영수증 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val receiptId: Long,
    @field:Schema(
        description = "영수증 사진 URL",
        example = "https://example.com/photo.jpg",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val photoUrl: String?,
)
