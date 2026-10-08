package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class FeedbackNotStartedException(
    code: ExceptionCode = SessionFeedbackExceptionCode.FEEDBACK_NOT_STARTED,
) : BusinessException(code)
