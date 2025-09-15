package com.server.dpmcore.session.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class SessionNotFoundException(
    code: ExceptionCode = SessionExceptionCode.SESSION_NOT_FOUND,
) : BusinessException(code)
