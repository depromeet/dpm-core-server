package core.application.member.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class CohortMembersNotFoundException(
    code: ExceptionCode = MemberExceptionCode.COHORT_MEMBERS_NOT_FOUND,
) : BusinessException(code)
