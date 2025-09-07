package com.server.dpmcore.gathering.gatheringReceipt.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberCountMustOverOneException : BusinessException(
    GatheringReceiptExceptionCode.MEMBER_COUNT_MUST_OVER_ONE,
)
