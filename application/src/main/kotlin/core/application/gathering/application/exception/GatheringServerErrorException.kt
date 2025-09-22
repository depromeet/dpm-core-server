package core.application.gathering.application.exception

import core.application.common.exception.BusinessException

class GatheringServerErrorException : BusinessException(
    GatheringExceptionCode.SERVER_ERROR,
)
