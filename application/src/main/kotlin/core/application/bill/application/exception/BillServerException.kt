package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillServerException : BusinessException(
    BillExceptionCode.SERVER_ERROR,
)
