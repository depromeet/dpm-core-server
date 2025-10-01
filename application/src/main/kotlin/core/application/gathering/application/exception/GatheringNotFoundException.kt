package core.application.gathering.application.exception

import core.application.common.exception.BusinessException

class GatheringNotFoundException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_FOUND,
)
