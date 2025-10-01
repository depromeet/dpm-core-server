package core.application.gathering.application.exception.receipt

import core.application.common.exception.BusinessException

class GatheringReceiptIdRequiredException : BusinessException(
    GatheringReceiptExceptionCode.GATHERING_RECEIPT_ID_REQUIRED,
)
