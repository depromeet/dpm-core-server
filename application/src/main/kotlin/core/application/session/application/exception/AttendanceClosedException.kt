package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AttendanceClosedException(
    code: ExceptionCode = SessionExceptionCode.ATTENDANCE_CLOSED,
) : BusinessException(code)
