package com.server.dpmcore.cohort.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class CohortNotFoundException(
    code: ExceptionCode = CohortExceptionCode.COHORT_NOT_FOUND,
) : BusinessException(code)
