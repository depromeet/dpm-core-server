package core.application.notification.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode

class InvalidNotificationTokenException(
    code: ExceptionCode = NotificationExceptionCode.INVALID_NOTIFICATION_TOKEN,
) : BusinessException(code)
