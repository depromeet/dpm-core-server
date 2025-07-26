package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberNameAuthorityCanNotBeNullException :
    BusinessException(
        MemberExceptionCode.MEMBER_NAME_AUTHORITY_CAN_NOT_BE_NULL,
    )
