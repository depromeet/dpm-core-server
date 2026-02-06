package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AnnouncementNotFoundException : BusinessException(
    AnnouncementExceptionCode.ANNOUNCEMENT_NOT_FOUND,
)
