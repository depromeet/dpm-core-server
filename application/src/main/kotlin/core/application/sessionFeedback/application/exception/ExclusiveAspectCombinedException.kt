package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class ExclusiveAspectCombinedException(
    code: ExceptionCode = SessionFeedbackExceptionCode.EXCLUSIVE_ASPECT_COMBINED,
) : BusinessException(code)
