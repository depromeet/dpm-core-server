package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class DuplicatedAspectException(
    code: ExceptionCode = SessionFeedbackExceptionCode.DUPLICATED_ASPECT,
) : BusinessException(code)
