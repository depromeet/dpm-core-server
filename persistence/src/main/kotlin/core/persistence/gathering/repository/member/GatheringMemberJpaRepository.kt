package core.persistence.gathering.repository.member

import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId
import core.entity.gathering.GatheringMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringMemberJpaRepository : JpaRepository<GatheringMemberEntity, Long> {
    fun findByGatheringId(gatheringId: GatheringId): List<GatheringMemberEntity>

    fun findByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): GatheringMemberEntity?
}
