package com.server.dpmcore.gathering.gatheringReceiptPhoto.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringReceiptPhotoIdRequiredException : BusinessException(
    GatheringReceiptPhotoExceptionCode.GATHERING_RECEIPT_PHOTO_ID_REQUIRED,
)
