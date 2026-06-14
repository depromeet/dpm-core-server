package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AbsenceReasonNotFoundException(
    code: ExceptionCode = AttendanceExceptionCode.ABSENCE_REASON_NOT_FOUND,
) : BusinessException(code)
