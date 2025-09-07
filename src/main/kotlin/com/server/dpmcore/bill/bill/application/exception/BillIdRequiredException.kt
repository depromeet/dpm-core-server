package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillIdRequiredException : BusinessException(
    BillExceptionCode.BILL_ID_REQUIRED,
)
