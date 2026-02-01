package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AnnouncementReadMarkedExist : BusinessException(
    AnnouncementExceptionCode.ANNOUNCEMENT_READ_MARKED_EXIST,
)
