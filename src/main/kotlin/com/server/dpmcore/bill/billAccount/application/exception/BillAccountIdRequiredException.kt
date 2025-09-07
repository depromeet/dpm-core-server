package com.server.dpmcore.bill.billAccount.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillAccountIdRequiredException : BusinessException(
    BillAccountExceptionCode.BILL_ACCOUNT_ID_REQUIRED,
)
