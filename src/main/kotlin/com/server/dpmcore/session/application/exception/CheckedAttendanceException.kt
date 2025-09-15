package com.server.dpmcore.session.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class CheckedAttendanceException(
    code: ExceptionCode = SessionExceptionCode.ALREADY_CHECKED_ATTENDANCE,
) : BusinessException(code)
