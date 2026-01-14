package com.server.dpmcore.member.member.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode
import com.server.dpmcore.member.member.application.exception.MemberExceptionCode

class InvalidMemberIdException(
    code: ExceptionCode = MemberExceptionCode.INVALID_MEMBER_ID,
) : BusinessException(code)
