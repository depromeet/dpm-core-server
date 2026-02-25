package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberDeletedException : BusinessException(
    MemberExceptionCode.MEMBER_DELETED,
)
