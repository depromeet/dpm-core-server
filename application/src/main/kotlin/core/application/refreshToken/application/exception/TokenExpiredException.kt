package core.application.refreshToken.application.exception

import core.application.common.exception.BusinessException

class TokenExpiredException : BusinessException(
    RefreshTokenExceptionCode.TOKEN_EXPIRED,
)
