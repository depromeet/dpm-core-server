package com.server.dpmcore.session.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class InvalidAttendanceCodeException(
    code: ExceptionCode = SessionExceptionCode.INVALID_ATTENDANCE_CODE,
) : BusinessException(code)
