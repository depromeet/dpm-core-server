package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.vo.AfterPartyId

interface AfterPartyQueryUseCase {
    fun getById(afterPartyId: AfterPartyId): AfterParty
}
