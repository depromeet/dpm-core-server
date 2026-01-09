package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberStatusAlreadyUpdatedException(
    code: MemberExceptionCode = MemberExceptionCode.MEMBER_STAUTS_ALREADY_UPDATED,
) : BusinessException(code)
