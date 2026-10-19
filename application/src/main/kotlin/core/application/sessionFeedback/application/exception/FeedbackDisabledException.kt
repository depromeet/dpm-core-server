package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class FeedbackDisabledException(
    code: ExceptionCode = SessionFeedbackExceptionCode.FEEDBACK_DISABLED,
) : BusinessException(code)
