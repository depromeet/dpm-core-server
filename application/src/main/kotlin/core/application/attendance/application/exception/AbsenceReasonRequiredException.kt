package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AbsenceReasonRequiredException(
    code: ExceptionCode = AttendanceExceptionCode.ABSENCE_REASON_REQUIRED,
) : BusinessException(code)
