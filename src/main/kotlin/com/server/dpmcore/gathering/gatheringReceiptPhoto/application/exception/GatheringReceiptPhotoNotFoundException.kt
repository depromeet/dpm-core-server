package com.server.dpmcore.gathering.gatheringReceiptPhoto.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringReceiptPhotoNotFoundException : BusinessException(
    GatheringReceiptPhotoExceptionCode.GATHERING_RECEIPT_PHOTO_NOT_FOUND,
)
