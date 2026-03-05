package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId

interface AfterPartyInviteTagQueryUseCase {
    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag>

    @Deprecated("최초 1회 회식 생성 태그에서 오류가 터지므로 Enum으로 관리 책임 이관")
    fun findAllDistinct(): List<AfterPartyInviteTag>

    fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag>
}
