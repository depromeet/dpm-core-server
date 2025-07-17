package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.BusinessException

class MemberNotFoundException :
    BusinessException(
        MemberExceptionCode.MEMBER_NOT_FOUND,
    )
