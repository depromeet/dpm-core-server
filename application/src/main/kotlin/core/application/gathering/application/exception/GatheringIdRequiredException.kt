package core.application.gathering.application.exception

import core.application.common.exception.BusinessException

class GatheringIdRequiredException : BusinessException(
    GatheringExceptionCode.GATHERING_ID_REQUIRED,
)
