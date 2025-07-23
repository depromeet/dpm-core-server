package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberIdCanNotBeNullException :
    BusinessException(
        MemberExceptionCode.MEMBER_ID_CAN_NOT_BE_NULL,
    )
