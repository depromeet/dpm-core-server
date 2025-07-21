package com.server.dpmcore.cohort.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class CohortNotFoundException(
    code: ExceptionCode,
) : BusinessException(code) {
    constructor() : this(CohortExceptionCode.COHORT_NOT_FOUND)

    companion object {
        val COHORT_NOT_FOUND = CohortExceptionCode.COHORT_NOT_FOUND
    }
}
