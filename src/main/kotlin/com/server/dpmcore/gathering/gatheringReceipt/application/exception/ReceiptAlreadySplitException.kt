package com.server.dpmcore.gathering.gatheringReceipt.application.exception

import com.server.dpmcore.common.exception.BusinessException

class ReceiptAlreadySplitException : BusinessException(
    GatheringReceiptExceptionCode.RECEIPT_ALREADY_SPLIT,
)
