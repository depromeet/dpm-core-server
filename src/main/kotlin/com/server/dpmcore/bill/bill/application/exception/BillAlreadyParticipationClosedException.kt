package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillAlreadyParticipationClosedException : BusinessException(
    BillExceptionCode.BILL_ALREADY_PARTICIPATION_CLOSED,
)
