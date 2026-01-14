package core.application.gathering.application.exception.member

import core.application.common.exception.BusinessException

class GatheringMemberNotFoundException : BusinessException(
    GatheringMemberExceptionCode.GATHERING_MEMBER_NOT_FOUND,
)
