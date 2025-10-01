package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillCannotCloseParticipationException : BusinessException(
    BillExceptionCode.BILL_CANNOT_CLOSE_PARTICIPATION,
)
