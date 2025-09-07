package com.server.dpmcore.gathering.gatheringMember.application.exception

import com.server.dpmcore.common.exception.BusinessException

class AlreadySubmittedInvitationException : BusinessException(
    GatheringMemberExceptionCode.ALREADY_SUBMITTED_INVITATION,
)
