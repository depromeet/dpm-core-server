package com.server.dpmcore.gathering.gathering.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringIdRequiredException : BusinessException(
    GatheringExceptionCode.GATHERING_ID_REQUIRED,
)
