package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.BusinessException

open class BillAccountException(
    code: BillAccountExceptionCode,
) : BusinessException(code) {
    class BillAccountServerException : BillAccountException(BillAccountExceptionCode.SERVER_ERROR)

    class BillAccountNotFoundException : BillAccountException(BillAccountExceptionCode.BILL_ACCOUNT_NOT_FOUND)

    class BillAccountIdRequiredException : BillAccountException(BillAccountExceptionCode.BILL_ACCOUNT_ID_REQUIRED)
}
