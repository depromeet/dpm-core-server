package com.server.dpmcore.gathering.gathering.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringNotParticipantMemberException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_PARTICIPANT_MEMBER,
)
