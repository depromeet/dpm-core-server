package com.server.dpmcore.gathering.exception

import com.server.dpmcore.common.exception.BusinessException

open class GatheringReceiptPhotoException(
    code: GatheringReceiptPhotoExceptionCode,
) : BusinessException(code) {
    class GatheringServerException : GatheringReceiptPhotoException(GatheringReceiptPhotoExceptionCode.SERVER_ERROR)

    class GatheringReceiptPhotoNotFoundException : GatheringReceiptPhotoException(
        GatheringReceiptPhotoExceptionCode.GATHERING_RECEIPT_PHOTO_NOT_FOUND,
    )

    class GatheringReceiptPhotoIdRequiredException : GatheringReceiptPhotoException(
        GatheringReceiptPhotoExceptionCode.GATHERING_RECEIPT_PHOTO_ID_REQUIRED,
    )
}
