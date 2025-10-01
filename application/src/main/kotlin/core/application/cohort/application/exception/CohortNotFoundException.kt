package core.application.cohort.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class CohortNotFoundException(
    code: ExceptionCode = CohortExceptionCode.COHORT_NOT_FOUND,
) : BusinessException(code)
