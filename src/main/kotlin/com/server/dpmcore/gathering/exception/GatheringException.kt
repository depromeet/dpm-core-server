package com.server.dpmcore.gathering.exception

import com.server.dpmcore.common.exception.BusinessException

open class GatheringException(
    code: GatheringExceptionCode,
) : BusinessException(code) {
    class GatheringServerException : GatheringException(GatheringExceptionCode.SERVER_ERROR)

    class GatheringRequiredException : GatheringException(GatheringExceptionCode.GATHERING_REQUIRED)

    class GatheringNotFoundException : GatheringException(GatheringExceptionCode.GATHERING_NOT_FOUND)

    class GatheringIdRequiredException : GatheringException(GatheringExceptionCode.GATHERING_ID_REQUIRED)

    class GatheringNotIncludedInBillException : GatheringException(
        GatheringExceptionCode.GATHERING_NOT_INCLUDED_IN_BILL,
    )

    class GatheringNotParticipantMemberException : GatheringException(
        GatheringExceptionCode.GATHERING_NOT_PARTICIPANT_MEMBER,
    )
}
