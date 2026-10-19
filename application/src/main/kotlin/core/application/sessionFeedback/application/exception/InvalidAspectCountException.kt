package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidAspectCountException(
    code: ExceptionCode = SessionFeedbackExceptionCode.INVALID_ASPECT_COUNT,
) : BusinessException(code)
