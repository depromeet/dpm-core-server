package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AnnouncementTypeCannotBeChangedException :
    BusinessException(
        AnnouncementExceptionCode.ANNOUNCEMENT_TYPE_CANNOT_BE_CHANGED,
    )
