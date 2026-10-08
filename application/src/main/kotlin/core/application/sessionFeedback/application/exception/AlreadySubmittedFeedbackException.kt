package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class AlreadySubmittedFeedbackException(
    code: ExceptionCode = SessionFeedbackExceptionCode.ALREADY_SUBMITTED_FEEDBACK,
) : BusinessException(code)
