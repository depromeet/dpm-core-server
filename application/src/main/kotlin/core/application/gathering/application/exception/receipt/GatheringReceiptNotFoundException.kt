package core.application.gathering.application.exception.receipt

import core.application.common.exception.BusinessException

class GatheringReceiptNotFoundException : BusinessException(
    GatheringReceiptExceptionCode.GATHERING_RECEIPT_NOT_FOUND,
)
