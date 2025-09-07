package com.server.dpmcore.gathering.gathering.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringRequiredException : BusinessException(
    GatheringExceptionCode.GATHERING_REQUIRED,
)
