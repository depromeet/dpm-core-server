package com.server.dpmcore.session.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class SessionNotFoundException(
    code: ExceptionCode,
) : BusinessException(code) {
    constructor() : this(SessionExceptionCode.SESSION_NOT_FOUND)

    companion object {
        val SESSION_NOT_FOUND = SessionExceptionCode.SESSION_NOT_FOUND
    }
}
