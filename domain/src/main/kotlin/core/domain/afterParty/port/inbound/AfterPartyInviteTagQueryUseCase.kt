package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId

interface AfterPartyInviteTagQueryUseCase {
    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag>

    fun findAllDistinct(): List<AfterPartyInviteTag>

    fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag>
}
