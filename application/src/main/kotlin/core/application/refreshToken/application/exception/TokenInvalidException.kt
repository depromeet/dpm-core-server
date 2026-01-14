package core.application.refreshToken.application.exception

import core.application.common.exception.BusinessException

class TokenInvalidException : BusinessException(
    RefreshTokenExceptionCode.TOKEN_INVALID,
)
