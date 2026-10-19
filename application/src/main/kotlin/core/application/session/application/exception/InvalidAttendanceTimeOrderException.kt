package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidAttendanceTimeOrderException(
    code: ExceptionCode = SessionExceptionCode.INVALID_ATTENDANCE_TIME_ORDER,
) : BusinessException(code)
