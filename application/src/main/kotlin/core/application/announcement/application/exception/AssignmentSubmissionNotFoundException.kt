package core.application.announcement.application.exception

import core.application.common.exception.BusinessException

class AssignmentSubmissionNotFoundException :
    BusinessException(
        AnnouncementExceptionCode.ASSIGNMENT_SUBMISSION_NOT_FOUND,
    )
