package core.application.bill.application.exception

import core.application.common.exception.BusinessException

class BillAlreadyParticipationClosedException : BusinessException(
    BillExceptionCode.BILL_ALREADY_PARTICIPATION_CLOSED,
)
