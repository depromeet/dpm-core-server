package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

/** 없는 이미지와 남의 이미지를 구분하지 않는다(존재 여부 노출 방지). */
class InvalidAbsenceReasonImageException(
    code: ExceptionCode = AttendanceExceptionCode.INVALID_ABSENCE_REASON_IMAGE,
) : BusinessException(code)
