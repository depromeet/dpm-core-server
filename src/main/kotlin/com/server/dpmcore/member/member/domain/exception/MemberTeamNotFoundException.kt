package com.server.dpmcore.member.member.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode
import com.server.dpmcore.member.member.application.exception.MemberExceptionCode

class MemberTeamNotFoundException(
    code: ExceptionCode = MemberExceptionCode.MEMBER_TEAM_NOT_FOUND,
) : BusinessException(code)
