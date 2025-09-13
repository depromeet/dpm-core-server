package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class MemberTeamNotFoundException(
    code: ExceptionCode = MemberExceptionCode.MEMBER_TEAM_NOT_FOUND,
) : BusinessException(code)
