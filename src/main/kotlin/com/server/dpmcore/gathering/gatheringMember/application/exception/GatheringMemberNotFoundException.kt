package com.server.dpmcore.gathering.gatheringMember.application.exception

import com.server.dpmcore.common.exception.BusinessException

class GatheringMemberNotFoundException : BusinessException(
    GatheringMemberExceptionCode.GATHERING_MEMBER_NOT_FOUND,
)
