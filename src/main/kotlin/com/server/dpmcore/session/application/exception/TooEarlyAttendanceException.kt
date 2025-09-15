package com.server.dpmcore.session.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class TooEarlyAttendanceException(
    code: ExceptionCode = SessionExceptionCode.TOO_EARLY_ATTENDANCE,
) : BusinessException(code)
