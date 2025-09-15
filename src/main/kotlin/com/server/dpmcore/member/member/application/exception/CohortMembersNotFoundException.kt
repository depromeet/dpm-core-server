package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class CohortMembersNotFoundException(
    code: ExceptionCode = MemberExceptionCode.COHORT_MEMBERS_NOT_FOUND,
) : BusinessException(code)
