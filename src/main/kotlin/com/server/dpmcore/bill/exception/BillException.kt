package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.BusinessException

open class BillException(
    code: BillExceptionCode,
) : BusinessException(code) {
    class BillAccountNotFoundException : BillException(BillExceptionCode.BILL_ACCOUNT_NOT_FOUND)

    class GatheringRequiredException : BillException(BillExceptionCode.GATHERING_REQUIRED)

    class BillAccountIdRequiredException : BillException(BillExceptionCode.BILL_ACCOUNT_ID_REQUIRED)

    class BillIdRequiredException : BillException(BillExceptionCode.BILL_ID_REQUIRED)

    class GatheringMembersRequiredException : BillException(BillExceptionCode.GATHERING_MEMBERS_REQUIRED)

    class BillNotFoundException : BillException(BillExceptionCode.BILL_NOT_FOUND)

    class GatheringNotFoundException : BillException(BillExceptionCode.GATHERING_NOT_FOUND)

    class BillCannotCloseParticipationException : BillException(BillExceptionCode.BILL_CANNOT_CLOSE_PARTICIPATION)
}
