package core.application.afterParty.application.exception

import core.application.common.exception.BusinessException

class AfterPartyNotFoundException :
    BusinessException(
        AfterPartyExceptionCode.AFTER_PARTY_NOT_FOUND,
    )
