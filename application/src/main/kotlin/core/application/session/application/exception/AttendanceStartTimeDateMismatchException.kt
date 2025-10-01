package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AttendanceStartTimeDateMismatchException(
    code: ExceptionCode = SessionExceptionCode.ATTENDANCE_START_TIME_DATE_MISMATCH,
) : BusinessException(code)
