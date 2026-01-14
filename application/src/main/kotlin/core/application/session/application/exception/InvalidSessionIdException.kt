package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidSessionIdException(
    code: ExceptionCode = SessionExceptionCode.INVALID_SESSION_ID,
) : BusinessException(code)
