package core.application.afterParty.application.exception

import core.application.common.exception.BusinessException

class AfterPartyIdRequiredException :
    BusinessException(
        AfterPartyExceptionCode.AFTER_PARTY_ID_REQUIRED,
    )
