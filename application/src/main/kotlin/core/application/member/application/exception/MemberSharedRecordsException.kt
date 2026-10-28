package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberSharedRecordsException(
    val blockedMemberIds: List<Long>,
) : BusinessException(MemberExceptionCode.MEMBER_SHARED_RECORDS_EXIST)
