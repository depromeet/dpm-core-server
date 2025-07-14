package com.server.dpmcore.refreshToken.application.exception

import com.server.dpmcore.common.exception.BusinessException

class TokenInvalidException : BusinessException(
    RefreshTokenExceptionCode.TOKEN_INVALID
)
