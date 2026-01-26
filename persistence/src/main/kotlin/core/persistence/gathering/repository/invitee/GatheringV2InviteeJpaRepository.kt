package core.persistence.gathering.repository.invitee

import core.domain.gathering.vo.GatheringV2Id
import core.entity.gathering.GatheringV2InviteeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringV2InviteeJpaRepository : JpaRepository<GatheringV2InviteeEntity, Long> {
    fun findByGatheringId(gatheringV2Id: GatheringV2Id): List<GatheringV2InviteeEntity>
}
