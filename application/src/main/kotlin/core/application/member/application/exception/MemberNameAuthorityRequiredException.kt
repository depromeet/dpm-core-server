package core.application.member.application.exception

import core.application.common.exception.BusinessException

class MemberNameAuthorityRequiredException :
    BusinessException(
        MemberExceptionCode.MEMBER_NAME_AUTHORITY_REQUIRED,
    )
