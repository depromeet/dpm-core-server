package com.server.dpmcore.gathering.gatheringReceipt.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringReceiptIdRequiredException : BusinessException(
    GatheringReceiptExceptionCode.GATHERING_RECEIPT_ID_REQUIRED,
)
