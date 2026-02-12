package core.application.member.application.exception

import core.application.common.exception.BusinessException

class InvalidEmailPasswordException : BusinessException(
    MemberExceptionCode.INVALID_EMAIL_PASSWORD,
)
