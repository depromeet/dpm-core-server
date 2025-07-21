package com.server.dpmcore.attendance.domain.exception

import com.server.dpmcore.common.exception.BusinessException
import com.server.dpmcore.common.exception.ExceptionCode

class AttendanceNotFoundException(
    code: ExceptionCode = AttendanceExceptionCode.ATTENDANCE_NOT_FOUND,
) : BusinessException(code)
