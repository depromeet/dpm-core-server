package core.application.cohort.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class CohortReferencedException(
    code: ExceptionCode = CohortExceptionCode.COHORT_REFERENCED,
) : BusinessException(code)
