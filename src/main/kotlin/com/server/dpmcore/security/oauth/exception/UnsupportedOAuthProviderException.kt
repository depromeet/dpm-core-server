package com.server.dpmcore.security.oauth.exception

import com.server.dpmcore.common.exception.BusinessException

class UnsupportedOAuthProviderException :
    BusinessException(
        OAuthExceptionCode.UNSUPPORTED_OAUTH_PROVIDER,
    )
