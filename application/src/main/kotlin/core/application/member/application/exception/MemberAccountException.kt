package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberAccountException(code: MemberExceptionCode) : BusinessException(code)
