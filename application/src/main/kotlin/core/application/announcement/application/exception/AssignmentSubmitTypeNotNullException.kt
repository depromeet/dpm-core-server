package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AssignmentSubmitTypeNotNullException :
    BusinessException(
        AnnouncementExceptionCode.ASSIGNMENT_SUBMIT_TYPE_NOT_NULL,
    )
