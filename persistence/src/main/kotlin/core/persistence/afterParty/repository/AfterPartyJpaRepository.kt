package core.persistence.afterParty.repository

import core.entity.afterParty.AfterPartyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AfterPartyJpaRepository : JpaRepository<AfterPartyEntity, Long> {
    @Query(
        """
        select party from AfterPartyEntity party
        where party.closedAt >= :now
          and exists (
              select tag.id from AfterPartyInviteTagEntity tag
              where tag.afterParty = party
                and tag.cohortId = :cohortId
                and tag.authorityId = :authorityId
          )
        """,
    )
    fun findOpenByInviteTag(
        @Param("cohortId") cohortId: Long,
        @Param("authorityId") authorityId: Long,
        @Param("now") now: Instant,
    ): List<AfterPartyEntity>

    fun findAllBy(): List<AfterPartyEntity>
}
