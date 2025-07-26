package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.updateQuery
import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import org.jooq.DSLContext
import org.jooq.generated.tables.references.AUTHORITIES
import org.jooq.generated.tables.references.GATHERING_MEMBERS
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.MEMBER_AUTHORITIES
import org.springframework.stereotype.Repository

@Repository
class GatheringMemberRepository(
    private val gatheringJpaRepository: GatheringMemberJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
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
        val id =
            gatheringMember.id?.value
                ?: throw GatheringException.GatheringIdRequiredException()

        queryFactory
            .updateQuery<GatheringMemberEntity> {
                where(col(GatheringMemberEntity::id).equal(id))
                set(col(GatheringMemberEntity::isChecked), gatheringMember.isChecked)
                set(col(GatheringMemberEntity::isJoined), gatheringMember.isJoined)
                set(col(GatheringMemberEntity::completedAt), gatheringMember.completedAt)
                set(col(GatheringMemberEntity::updatedAt), gatheringMember.updatedAt)
                set(col(GatheringMemberEntity::deletedAt), gatheringMember.deletedAt)
            }.executeUpdate()
    }

    override fun findMemberIdsByGatheringId(gatheringId: GatheringId): List<MemberId> =
        dsl
            .select(GATHERING_MEMBERS.MEMBER_ID)
            .from(GATHERING_MEMBERS)
            .where(GATHERING_MEMBERS.GATHERING_ID.eq(gatheringId.value))
            .fetch(GATHERING_MEMBERS.MEMBER_ID)
            .map { MemberId(it ?: throw GatheringException.GatheringIdRequiredException()) }

    override fun findGatheringMemberWithIsJoinByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): GatheringMemberIsJoinQueryModel {
        val query =
            dsl
                .select(
                    MEMBERS.NAME,
                    MEMBERS.PART,
                    AUTHORITIES.NAME,
                    GATHERING_MEMBERS.IS_JOINED,
                ).from(GATHERING_MEMBERS)
                .join(MEMBERS)
                .on(GATHERING_MEMBERS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .join(MEMBER_AUTHORITIES)
                .on(MEMBERS.MEMBER_ID.eq(MEMBER_AUTHORITIES.MEMBER_ID))
                .join(AUTHORITIES)
                .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
                .where(
                    GATHERING_MEMBERS.GATHERING_ID
                        .eq(gatheringId.value)
                        .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
                ).fetchOne() ?: throw GatheringMemberException.GatheringMemberNotFoundException()

        val memberName = query.get(MEMBERS.NAME)
        val memberPart = query.get(MEMBERS.PART)
        val authorityName = query.get(AUTHORITIES.NAME)
        val isJoined = query.get(GATHERING_MEMBERS.IS_JOINED)

        if (memberName == null || authorityName == null || isJoined == null) {
            throw GatheringMemberException.GatheringMemberNotFoundException()
        }

        return GatheringMemberIsJoinQueryModel(
            name = memberName,
            part = memberPart,
            authority = authorityName,
            isJoined = isJoined,
        )
    }
}
