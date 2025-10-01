package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberIdRequiredException :
    BusinessException(
        MemberExceptionCode.MEMBER_ID_REQUIRED,
    )
