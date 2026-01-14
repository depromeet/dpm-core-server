package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillNotFoundException : BusinessException(
    BillExceptionCode.BILL_NOT_FOUND,
)
