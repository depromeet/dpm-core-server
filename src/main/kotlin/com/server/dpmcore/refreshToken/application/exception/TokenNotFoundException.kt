package com.server.dpmcore.refreshToken.application.exception

import com.server.dpmcore.common.exception.BusinessException

class TokenNotFoundException : BusinessException(
    RefreshTokenExceptionCode.TOKEN_NOT_FOUND,
)
