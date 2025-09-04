package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillCannotCloseParticipationException : BusinessException(
    BillExceptionCode.BILL_CANNOT_CLOSE_PARTICIPATION,
)
