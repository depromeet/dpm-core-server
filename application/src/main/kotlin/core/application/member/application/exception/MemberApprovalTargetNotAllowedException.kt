package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberApprovalTargetNotAllowedException : BusinessException(
    MemberExceptionCode.MEMBER_APPROVAL_TARGET_NOT_ALLOWED,
)
