package com.server.dpmcore.gathering.gatheringReceipt.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringReceiptNotFoundException : BusinessException(
    GatheringReceiptExceptionCode.GATHERING_RECEIPT_NOT_FOUND,
)
