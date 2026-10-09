package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberManagementTargetNotAllowedException : BusinessException(
    MemberExceptionCode.MEMBER_MANAGEMENT_TARGET_NOT_ALLOWED,
)
