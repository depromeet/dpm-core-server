package core.application.member.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class MemberTeamNotFoundException(
    code: ExceptionCode = MemberExceptionCode.MEMBER_TEAM_NOT_FOUND,
) : BusinessException(code)
