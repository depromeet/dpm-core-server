package core.application.bill.application.exception.account

import core.application.common.exception.BusinessException

class BillAccountNotFoundException : BusinessException(
    BillAccountExceptionCode.BILL_ACCOUNT_NOT_FOUND,
)
