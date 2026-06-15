package core.application.member.application.exception

import core.application.common.exception.BusinessException

class AppleLoginMemberRequiredException :
    BusinessException(
        MemberExceptionCode.APPLE_LOGIN_MEMBER_REQUIRED,
    )
