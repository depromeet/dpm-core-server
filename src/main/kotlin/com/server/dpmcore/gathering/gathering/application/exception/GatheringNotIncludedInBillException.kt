package com.server.dpmcore.gathering.gathering.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringNotIncludedInBillException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_INCLUDED_IN_BILL,
)
