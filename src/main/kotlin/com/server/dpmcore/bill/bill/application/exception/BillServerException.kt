package com.server.dpmcore.bill.bill.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillServerException : BusinessException(
    BillExceptionCode.SERVER_ERROR,
)
