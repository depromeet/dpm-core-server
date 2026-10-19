package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AttendanceAlreadyDecidedException(
    code: ExceptionCode = SessionExceptionCode.ATTENDANCE_ALREADY_DECIDED,
) : BusinessException(code)
