package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class NotAnAssignmentException :
    BusinessException(
    AnnouncementExceptionCode.NOT_AN_ASSIGNMENT_EXCEPTION
)
