package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class NotFeedbackTargetException(
    code: ExceptionCode = SessionFeedbackExceptionCode.NOT_FEEDBACK_TARGET,
) : BusinessException(code)
