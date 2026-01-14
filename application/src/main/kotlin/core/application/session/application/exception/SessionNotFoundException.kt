package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class SessionNotFoundException(
    code: ExceptionCode = SessionExceptionCode.SESSION_NOT_FOUND,
) : BusinessException(code)
