package core.application.notification.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class NotificationTokenNotFoundException(
    code: ExceptionCode = NotificationExceptionCode.NOTIFICATION_TOKEN_NOT_FOUND,
) : BusinessException(code)
