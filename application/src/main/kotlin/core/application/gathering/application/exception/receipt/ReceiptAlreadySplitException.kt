package core.application.gathering.application.exception.receipt

import core.application.common.exception.BusinessException

class ReceiptAlreadySplitException : BusinessException(
    GatheringReceiptExceptionCode.RECEIPT_ALREADY_SPLIT,
)
