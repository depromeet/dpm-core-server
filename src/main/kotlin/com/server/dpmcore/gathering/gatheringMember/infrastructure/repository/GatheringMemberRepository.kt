package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.updateQuery
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Repository

@Repository
class GatheringMemberRepository(
    private val gatheringJpaRepository: GatheringMemberJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : GatheringMemberPersistencePort {
    override fun save(
        gatheringMember: GatheringMember,
        gathering: Gathering,
    ) {
        gatheringJpaRepository.save(GatheringMemberEntity.from(gatheringMember, gathering))
    }

    override fun findByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringJpaRepository.findByGatheringId(gatheringId).map { it.toDomain() }

    override fun findByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<GatheringMember> =
        gatheringJpaRepository.findByGatheringIdAndMemberId(gatheringId, memberId).map { it.toDomain() }

    override fun updateGatheringMemberById(gatheringMember: GatheringMember) {
        queryFactory
            .updateQuery<GatheringMemberEntity> {
                where(
                    gatheringMember.id?.let { col(GatheringMemberEntity::id).equal(it.value) },
                )
                set(col(GatheringMemberEntity::isChecked), gatheringMember.isChecked)
                set(col(GatheringMemberEntity::isJoined), gatheringMember.isJoined)
                set(col(GatheringMemberEntity::completedAt), gatheringMember.completedAt)
                set(col(GatheringMemberEntity::updatedAt), gatheringMember.updatedAt)
                set(col(GatheringMemberEntity::deletedAt), gatheringMember.deletedAt)
            }.executeUpdate()
    }
}
