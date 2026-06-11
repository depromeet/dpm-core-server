package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class MemberAttendanceAmbiguousException(
    code: ExceptionCode = AttendanceExceptionCode.MEMBER_ATTENDANCE_AMBIGUOUS,
) : BusinessException(code)
