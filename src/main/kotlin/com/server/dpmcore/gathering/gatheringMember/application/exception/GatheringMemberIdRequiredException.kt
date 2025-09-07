package com.server.dpmcore.gathering.gatheringMember.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringMemberIdRequiredException : BusinessException(
    GatheringMemberExceptionCode.GATHERING_MEMBER_ID_REQUIRED,
)
