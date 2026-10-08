package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidFeedbackStartAtException(
    code: ExceptionCode = SessionFeedbackExceptionCode.INVALID_FEEDBACK_START_AT,
) : BusinessException(code)
