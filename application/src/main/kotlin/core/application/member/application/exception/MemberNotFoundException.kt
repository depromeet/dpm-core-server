package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberNotFoundException :
    BusinessException(
        MemberExceptionCode.MEMBER_NOT_FOUND,
    )
