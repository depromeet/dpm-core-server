package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberNameAuthorityRequiredException :
    BusinessException(
        MemberExceptionCode.MEMBER_NAME_AUTHORITY_REQUIRED,
    )
