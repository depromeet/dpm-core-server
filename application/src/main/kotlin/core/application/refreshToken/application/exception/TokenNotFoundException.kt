package core.application.refreshToken.application.exception

import core.application.common.exception.BusinessException

class TokenNotFoundException : BusinessException(
    RefreshTokenExceptionCode.TOKEN_NOT_FOUND,
)
