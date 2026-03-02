package core.application.afterParty.application.service

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.port.inbound.AfterPartyInviteTagQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteTagPersistencePort
import core.domain.afterParty.vo.AfterPartyId
import org.springframework.stereotype.Service

@Service
class AfterPartyInviteTagQueryService(
    private val afterPartyInviteTagPersistencePort: AfterPartyInviteTagPersistencePort,
) : AfterPartyInviteTagQueryUseCase {
    override fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag> =
        afterPartyInviteTagPersistencePort.findByAfterPartyId(afterPartyId)

    override fun findAllDistinct(): List<AfterPartyInviteTag> = afterPartyInviteTagPersistencePort.findAllDistinct()

    override fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag> =
        afterPartyInviteTagPersistencePort.findDistinctByTagName(tagName)
}
