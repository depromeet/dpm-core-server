package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AbsenceReasonImageAlreadyAttachedException(
    code: ExceptionCode = AttendanceExceptionCode.ABSENCE_REASON_IMAGE_ALREADY_ATTACHED,
) : BusinessException(code)
