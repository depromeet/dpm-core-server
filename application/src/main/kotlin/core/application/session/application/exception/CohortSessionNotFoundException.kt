package core.application.session.application.exception

import core.application.common.exception.BusinessException

class CohortSessionNotFoundException(
    code: SessionExceptionCode = SessionExceptionCode.COHORT_SESSION_NOT_FOUND,
) : BusinessException(code)
