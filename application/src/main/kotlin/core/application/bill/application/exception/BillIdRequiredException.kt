package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillIdRequiredException : BusinessException(
    BillExceptionCode.BILL_ID_REQUIRED,
)
