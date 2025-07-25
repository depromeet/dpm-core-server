package com.server.dpmcore.gathering.exception

import com.server.dpmcore.common.exception.BusinessException

open class GatheringReceiptException(
    code: GatheringReceiptExceptionCode,
) : BusinessException(code) {
    class GatheringServerException : GatheringReceiptException(GatheringReceiptExceptionCode.SERVER_ERROR)

    class GatheringReceiptNotFoundException : GatheringReceiptException(
        GatheringReceiptExceptionCode.GATHERING_RECEIPT_NOT_FOUND,
    )

    class GatheringReceiptIdRequiredException : GatheringReceiptException(
        GatheringReceiptExceptionCode.GATHERING_RECEIPT_ID_REQUIRED,
    )

    class MemberCountMustOverOneException : GatheringReceiptException(
        GatheringReceiptExceptionCode.MEMBER_COUNT_MUST_OVER_ONE,
    )

    class ReceiptAlreadySplitException : GatheringReceiptException(GatheringReceiptExceptionCode.RECEIPT_ALREADY_SPLIT)
}
