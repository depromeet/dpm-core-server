package core.application.afterParty.application.exception.member

import core.application.common.exception.BusinessException

class AfterPartyMemberNotFoundException :
    BusinessException(
        AfterPartyMemberExceptionCode.AFTER_PARTY_MEMBER_NOT_FOUND,
    )
