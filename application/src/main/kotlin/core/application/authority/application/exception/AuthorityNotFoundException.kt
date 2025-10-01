package core.application.authority.application.exception

import core.application.common.exception.BusinessException

class AuthorityNotFoundException :
    BusinessException(
        AuthorityExceptionCode.AUTHORITY_NOT_FOUND,
    )
