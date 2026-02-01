package core.application.security.oauth.exception

import core.application.common.exception.BusinessException

class InvalidAccessTokenException(
    exceptionCode: JwtExceptionCode = JwtExceptionCode.TOKEN_INVALID,
) : BusinessException(exceptionCode)
