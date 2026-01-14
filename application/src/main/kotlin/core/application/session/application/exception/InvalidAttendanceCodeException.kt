package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidAttendanceCodeException(
    code: ExceptionCode = SessionExceptionCode.INVALID_ATTENDANCE_CODE,
) : BusinessException(code)
