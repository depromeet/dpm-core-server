package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AbsenceReasonTooLongException(
    code: ExceptionCode = AttendanceExceptionCode.ABSENCE_REASON_TOO_LONG,
) : BusinessException(code)
