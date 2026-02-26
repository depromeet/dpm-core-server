package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AssignmentNotFoundException :
    BusinessException(
        AnnouncementExceptionCode.ASSIGNMENT_NOT_FOUND,
    )
