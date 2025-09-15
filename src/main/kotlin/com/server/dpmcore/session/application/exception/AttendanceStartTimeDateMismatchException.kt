package com.server.dpmcore.session.application.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class AttendanceStartTimeDateMismatchException(
    code: ExceptionCode = SessionExceptionCode.ATTENDANCE_START_TIME_DATE_MISMATCH,
) : BusinessException(code)
