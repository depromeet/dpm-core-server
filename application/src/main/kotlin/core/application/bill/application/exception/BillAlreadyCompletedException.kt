package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillAlreadyCompletedException : BusinessException(
    BillExceptionCode.BILL_ALREADY_COMPLETED,
)
