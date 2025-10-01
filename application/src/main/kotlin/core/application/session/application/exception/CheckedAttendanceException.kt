package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class CheckedAttendanceException(
    code: ExceptionCode = SessionExceptionCode.ALREADY_CHECKED_ATTENDANCE,
) : BusinessException(code)
