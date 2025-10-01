package core.application.bill.application.exception.account

import core.application.common.exception.BusinessException

class BillAccountIdRequiredException : BusinessException(
    BillAccountExceptionCode.BILL_ACCOUNT_ID_REQUIRED,
)
