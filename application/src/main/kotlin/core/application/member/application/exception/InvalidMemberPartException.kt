package core.application.member.application.exception

import core.application.common.exception.BusinessException

class InvalidMemberPartException :
    BusinessException(
        MemberExceptionCode.INVALID_MEMBER_PART,
    )
