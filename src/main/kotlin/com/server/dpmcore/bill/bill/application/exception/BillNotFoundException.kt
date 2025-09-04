package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillNotFoundException : BusinessException(
    BillExceptionCode.BILL_NOT_FOUND,
)
