package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class PartialAttendanceTimesException(
    code: ExceptionCode = SessionExceptionCode.PARTIAL_ATTENDANCE_TIMES,
) : BusinessException(code)
