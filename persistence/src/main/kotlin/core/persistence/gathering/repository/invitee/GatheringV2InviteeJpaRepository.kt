package core.persistence.gathering.repository.invitee

import core.entity.gathering.GatheringV2InviteeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringV2InviteeJpaRepository : JpaRepository<GatheringV2InviteeEntity, Long>
