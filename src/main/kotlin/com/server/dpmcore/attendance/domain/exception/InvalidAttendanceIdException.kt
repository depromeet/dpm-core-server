package com.server.dpmcore.attendance.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class InvalidAttendanceIdException(
    code: ExceptionCode = AttendanceExceptionCode.INVALID_ATTENDANCE_ID,
) : BusinessException(code)
