package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class FeedbackClosedException(
    code: ExceptionCode = SessionFeedbackExceptionCode.FEEDBACK_CLOSED,
) : BusinessException(code)
