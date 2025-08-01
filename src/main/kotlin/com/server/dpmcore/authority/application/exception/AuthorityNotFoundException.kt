package com.server.dpmcore.authority.application.exception

import com.server.dpmcore.common.exception.BusinessException

class AuthorityNotFoundException :
    BusinessException(
        AuthorityExceptionCode.AUTHORITY_NOT_FOUND,
    )
