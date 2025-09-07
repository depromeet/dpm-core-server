package com.server.dpmcore.bill.billAccount.application.exception

import com.server.dpmcore.common.exception.BusinessException

class BillAccountNotFoundException : BusinessException(
    BillAccountExceptionCode.BILL_ACCOUNT_NOT_FOUND,
)
