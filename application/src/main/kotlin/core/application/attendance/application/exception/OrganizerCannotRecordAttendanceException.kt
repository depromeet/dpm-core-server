package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class OrganizerCannotRecordAttendanceException(
    code: ExceptionCode = AttendanceExceptionCode.ORGANIZER_CANNOT_RECORD_ATTENDANCE,
) : BusinessException(code)
