package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AttendanceNotFoundException(
    code: ExceptionCode = AttendanceExceptionCode.ATTENDANCE_NOT_FOUND,
) : BusinessException(code)
