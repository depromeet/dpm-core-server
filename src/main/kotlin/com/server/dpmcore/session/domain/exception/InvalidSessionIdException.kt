package com.server.dpmcore.session.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class InvalidSessionIdException(
    code: ExceptionCode = SessionExceptionCode.INVALID_SESSION_ID,
) : BusinessException(code)
