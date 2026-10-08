package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class FeedbackStartAtRequiredException(
    code: ExceptionCode = SessionFeedbackExceptionCode.FEEDBACK_START_AT_REQUIRED,
) : BusinessException(code)
