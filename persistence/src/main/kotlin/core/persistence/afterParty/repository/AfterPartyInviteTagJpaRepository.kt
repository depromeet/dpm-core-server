package core.persistence.afterParty.repository

import core.entity.afterParty.AfterPartyInviteTagEntity
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("FunctionName")
interface AfterPartyInviteTagJpaRepository : JpaRepository<AfterPartyInviteTagEntity, Long> {
    fun findByAfterPartyId(afterPartyId: Long): List<AfterPartyInviteTagEntity>

    fun deleteByAfterPartyId(afterPartyId: Long)
}
