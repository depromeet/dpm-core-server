package core.application.gathering.application.exception

import core.application.common.exception.BusinessException

class GatheringNotParticipantMemberException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_PARTICIPANT_MEMBER,
)
