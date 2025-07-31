package com.server.dpmcore.member.member.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode
import com.server.dpmcore.member.member.application.exception.MemberExceptionCode

class CohortMembersNotFoundException(
    code: ExceptionCode = MemberExceptionCode.COHORT_MEMBERS_NOT_FOUND,
) : BusinessException(code)
