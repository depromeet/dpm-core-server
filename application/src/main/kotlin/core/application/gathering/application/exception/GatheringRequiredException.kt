package core.application.gathering.application.exception

import core.application.common.exception.BusinessException

class GatheringRequiredException : BusinessException(
    GatheringExceptionCode.GATHERING_REQUIRED,
)
