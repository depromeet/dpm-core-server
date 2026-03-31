package core.application.cohort.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class CohortAlreadyExistsException(
    code: ExceptionCode = CohortExceptionCode.COHORT_ALREADY_EXISTS,
) : BusinessException(code)
