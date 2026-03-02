package core.persistence.afterParty.repository

import core.entity.afterParty.AfterPartyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AfterPartyJpaRepository : JpaRepository<AfterPartyEntity, Long> {
    fun findAllBy(): List<AfterPartyEntity>
}
