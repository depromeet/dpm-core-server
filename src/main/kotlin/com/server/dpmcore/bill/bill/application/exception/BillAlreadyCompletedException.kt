package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillAlreadyCompletedException : BusinessException(
    BillExceptionCode.BILL_ALREADY_COMPLETED,
)
