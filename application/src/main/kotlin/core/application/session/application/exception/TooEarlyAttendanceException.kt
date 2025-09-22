package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class TooEarlyAttendanceException(
    code: ExceptionCode = SessionExceptionCode.TOO_EARLY_ATTENDANCE,
) : BusinessException(code)
