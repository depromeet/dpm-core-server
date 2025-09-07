package com.server.dpmcore.gathering.gathering.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringNotFoundException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_FOUND,
)
