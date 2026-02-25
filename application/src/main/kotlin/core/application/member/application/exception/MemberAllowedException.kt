package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberAllowedException : BusinessException(
    MemberExceptionCode.MEMBER_NOT_ALLOWED,
)
