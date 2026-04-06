package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberLoginMethodAlreadyLinkedException :
    BusinessException(
        MemberExceptionCode.MEMBER_LOGIN_METHOD_ALREADY_LINKED,
    )
