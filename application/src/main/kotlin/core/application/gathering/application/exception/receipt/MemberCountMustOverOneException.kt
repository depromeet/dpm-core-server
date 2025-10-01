package core.application.gathering.application.exception.receipt

import core.application.common.exception.BusinessException

class MemberCountMustOverOneException : BusinessException(
    GatheringReceiptExceptionCode.MEMBER_COUNT_MUST_OVER_ONE,
)
