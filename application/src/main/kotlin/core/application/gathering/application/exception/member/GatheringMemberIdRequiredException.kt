package core.application.gathering.application.exception.member

import core.application.common.exception.BusinessException

class GatheringMemberIdRequiredException : BusinessException(
    GatheringMemberExceptionCode.GATHERING_MEMBER_ID_REQUIRED,
)
