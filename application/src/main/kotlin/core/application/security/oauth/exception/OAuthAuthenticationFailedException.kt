package core.application.security.oauth.exception

import core.application.common.exception.BusinessException

class OAuthAuthenticationFailedException(
    code: OAuthExceptionCode = OAuthExceptionCode.AUTHENTICATION_FAILED,
) : BusinessException(code)
