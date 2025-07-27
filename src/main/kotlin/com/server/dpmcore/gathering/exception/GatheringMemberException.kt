package com.server.dpmcore.gathering.exception

import com.server.dpmcore.common.exception.BusinessException

open class GatheringMemberException(
    code: GatheringMemberExceptionCode,
) : BusinessException(code) {
    class GatheringServerException : GatheringMemberException(GatheringMemberExceptionCode.SERVER_ERROR)

    class GatheringMemberNotFoundException : GatheringMemberException(
        GatheringMemberExceptionCode.GATHERING_MEMBER_NOT_FOUND,
    )

    class GatheringMemberRequiredException : GatheringMemberException(
        GatheringMemberExceptionCode.GATHERING_MEMBER_REQUIRED,
    )

    class AlreadyConfirmedMemberException : GatheringMemberException(
        GatheringMemberExceptionCode.ALREADY_CONFIRMED_MEMBER,
    )

    class GatheringMemberIdRequiredException : GatheringMemberException(
        GatheringMemberExceptionCode.GATHERING_MEMBER_ID_REQUIRED,
    )
}
