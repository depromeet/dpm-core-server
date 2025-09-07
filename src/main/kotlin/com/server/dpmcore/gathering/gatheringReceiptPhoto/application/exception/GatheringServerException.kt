package com.server.dpmcore.gathering.gatheringReceiptPhoto.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringServerException : BusinessException(
    GatheringReceiptPhotoExceptionCode.SERVER_ERROR,
)
