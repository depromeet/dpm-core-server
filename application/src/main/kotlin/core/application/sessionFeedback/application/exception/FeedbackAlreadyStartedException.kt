package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class FeedbackAlreadyStartedException(
    code: ExceptionCode = SessionFeedbackExceptionCode.FEEDBACK_ALREADY_STARTED,
) : BusinessException(code)
