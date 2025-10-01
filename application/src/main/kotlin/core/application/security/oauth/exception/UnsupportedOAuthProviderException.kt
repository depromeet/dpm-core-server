package core.application.security.oauth.exception

import core.application.common.exception.BusinessException

class UnsupportedOAuthProviderException :
    BusinessException(
        OAuthExceptionCode.UNSUPPORTED_OAUTH_PROVIDER,
    )
