package core.application.notification.application.exception

import core.application.common.exception.BusinessException

class NotificationFailedException :
    BusinessException(
        NotificationExceptionCode.NOTIFICATION_FAILED,
    )
