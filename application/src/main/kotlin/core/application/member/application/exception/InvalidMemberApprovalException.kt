package core.application.member.application.exception

import core.application.common.exception.BusinessException

class InvalidMemberApprovalException : BusinessException(MemberExceptionCode.INVALID_MEMBER_APPROVAL)
