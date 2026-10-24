package core.application.session.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class DuplicateSessionDateException(
    code: ExceptionCode = SessionExceptionCode.DUPLICATE_SESSION_DATE,
) : BusinessException(code)
