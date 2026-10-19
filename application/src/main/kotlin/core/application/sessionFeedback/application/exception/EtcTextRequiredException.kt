package core.application.sessionFeedback.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class EtcTextRequiredException(
    code: ExceptionCode = SessionFeedbackExceptionCode.ETC_TEXT_REQUIRED,
) : BusinessException(code)
