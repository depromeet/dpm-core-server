package core.application.gathering.application.exception

import core.application.common.exception.BusinessException


class GatheringNotIncludedInBillException : BusinessException(
    GatheringExceptionCode.GATHERING_NOT_INCLUDED_IN_BILL,
)
