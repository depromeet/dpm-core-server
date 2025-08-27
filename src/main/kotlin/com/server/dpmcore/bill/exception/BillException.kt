package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.BusinessException

open class BillException(
    code: BillExceptionCode,
) : BusinessException(code) {
    class BillServerException : BillException(BillExceptionCode.SERVER_ERROR)

    class BillNotFoundException : BillException(BillExceptionCode.BILL_NOT_FOUND)

    class BillIdRequiredException : BillException(BillExceptionCode.BILL_ID_REQUIRED)

    class BillCannotCloseParticipationException : BillException(BillExceptionCode.BILL_CANNOT_CLOSE_PARTICIPATION)

    class BillAlreadyCompletedException : BillException(BillExceptionCode.BILL_ALREADY_COMPLETED)

    class BillAlreadyParticipationClosedException : BillException(BillExceptionCode.BILL_ALREADY_PARTICIPATION_CLOSED)

    class BillAlreadyPendingException : BillException(BillExceptionCode.BILL_ALREADY_PENDING)

    class BillAlreadyOpenException : BillException(BillExceptionCode.BILL_ALREADY_OPEN)

    class BillAlreadyInProgressException : BillException(BillExceptionCode.BILL_ALREADY_IN_PROGRESS)
}
