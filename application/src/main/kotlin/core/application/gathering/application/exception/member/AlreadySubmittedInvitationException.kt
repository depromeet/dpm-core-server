package core.application.gathering.application.exception.member

import core.application.common.exception.BusinessException

class AlreadySubmittedInvitationException : BusinessException(
    GatheringMemberExceptionCode.ALREADY_SUBMITTED_INVITATION,
)
