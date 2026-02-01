package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AnnouncementReadNotFoundException : BusinessException(
    AnnouncementExceptionCode.ANNOUNCEMENT_READ_NOT_FOUND,
)
