package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberIdRequiredException :
    BusinessException(
        MemberExceptionCode.MEMBER_ID_REQUIRED,
    )
