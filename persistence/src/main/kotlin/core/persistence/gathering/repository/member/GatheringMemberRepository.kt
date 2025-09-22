package core.persistence.gathering.repository.member

import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotFoundException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.application.exception.GatheringMemberNotFoundException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.presentation.mapper.TimeMapper.localDateTimeToInstant
import org.jooq.DSLContext
import org.jooq.generated.tables.references.AUTHORITIES
import org.jooq.generated.tables.references.GATHERING_MEMBERS
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.MEMBER_AUTHORITIES
import org.jooq.generated.tables.references.MEMBER_TEAMS
import org.jooq.generated.tables.references.TEAMS
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GatheringMemberRepository(
    private val gatheringJpaRepository: GatheringMemberJpaRepository,
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
    ): GatheringMember =
        gatheringJpaRepository.findByGatheringIdAndMemberId(gatheringId, memberId)?.toDomain()
            ?: throw GatheringMemberNotFoundException()

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringMember는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateIsViewedById(memberId: Long) {
        dsl
            .update(GATHERING_MEMBERS)
            .set(GATHERING_MEMBERS.IS_VIEWED, true)
            .set(GATHERING_MEMBERS.UPDATED_AT, LocalDateTime.now())
            .where(GATHERING_MEMBERS.MEMBER_ID.eq(memberId))
            .execute()
    }

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringMember는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateIsJoinedById(gatheringMember: GatheringMember) {
        dsl
            .update(GATHERING_MEMBERS)
            .set(GATHERING_MEMBERS.IS_JOINED, gatheringMember.isJoined)
            .set(GATHERING_MEMBERS.UPDATED_AT, LocalDateTime.now())
            .where(GATHERING_MEMBERS.GATHERING_MEMBER_ID.eq(gatheringMember.id?.value))
            .execute()
    }

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringMember는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateDepositAndMemoById(
        gatheringMember: GatheringMember,
        isDeposit: Boolean,
        memo: String?,
    ) {
        val now = LocalDateTime.now()

        dsl
            .update(GATHERING_MEMBERS)
            .set(GATHERING_MEMBERS.UPDATED_AT, now)
            .set(GATHERING_MEMBERS.MEMO, memo)
            .set(
                GATHERING_MEMBERS.COMPLETED_AT,
                if (isDeposit) now else null,
            )
            .where(GATHERING_MEMBERS.GATHERING_MEMBER_ID.eq(gatheringMember.id?.value))
            .execute()
    }

    override fun findMemberIdsByGatheringId(gatheringId: GatheringId): List<MemberId> =
        dsl
            .select(GATHERING_MEMBERS.MEMBER_ID)
            .from(GATHERING_MEMBERS)
            .where(GATHERING_MEMBERS.GATHERING_ID.eq(gatheringId.value))
            .fetch(GATHERING_MEMBERS.MEMBER_ID)
            .map { MemberId(it ?: throw GatheringMemberNotFoundException()) }

    override fun findGatheringMemberWithIsJoinByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<GatheringMemberIsJoinQueryModel> {
        val queryResults =
            dsl
                .select(
                    MEMBERS.NAME,
                    TEAMS.NUMBER,
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
                .leftJoin(MEMBER_TEAMS)
                .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .leftJoin(TEAMS)
                .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                .where(
                    GATHERING_MEMBERS.GATHERING_ID
                        .eq(gatheringId.value)
                        .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
                ).fetch()
        return queryResults.map { query ->

            val memberName = query.get(MEMBERS.NAME)
            val teamNumber = query.get(TEAMS.NUMBER)
            val memberPart = query.get(MEMBERS.PART)
            val authorityName = query.get(AUTHORITIES.NAME)
            val isJoined = query.get(GATHERING_MEMBERS.IS_JOINED)

            if (memberName == null || authorityName == null || isJoined == null) {
                throw GatheringMemberNotFoundException()
            }
            GatheringMemberIsJoinQueryModel(
                name = memberName,
                teamNumber = teamNumber ?: 0,
                part = memberPart,
                authority = authorityName,
                isJoined = isJoined,
            )
        }
    }

    /**
     * JPA를 통해 루트 엔티티를 fetch하고 Dirty Checking을 통해 업데이트하면 불필요한 조회가 발생함.
     *
     * GatheringMember는 애그리거트 하위 도메인이므로, 조회 없이 jOOQ로 바로 업데이트.
     *
     * @author LeeHanEum
     * @since 2025.09.04
     */
    override fun updateIsInvitationSubmittedById(memberId: Long) {
        dsl
            .update(GATHERING_MEMBERS)
            .set(GATHERING_MEMBERS.IS_INVITATION_SUBMITTED, true)
            .set(GATHERING_MEMBERS.UPDATED_AT, LocalDateTime.now())
            .where(GATHERING_MEMBERS.MEMBER_ID.eq(memberId))
            .execute()
    }

    override fun findGatheringMemberWithIsInvitationSubmittedByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<BillMemberIsInvitationSubmittedQueryModel> {
        val queryResults =
            dsl
                .select(
                    MEMBERS.NAME,
                    TEAMS.NUMBER,
                    MEMBERS.PART,
                    AUTHORITIES.NAME,
                    GATHERING_MEMBERS.IS_INVITATION_SUBMITTED,
                ).from(GATHERING_MEMBERS)
                .join(MEMBERS)
                .on(GATHERING_MEMBERS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .join(MEMBER_AUTHORITIES)
                .on(MEMBERS.MEMBER_ID.eq(MEMBER_AUTHORITIES.MEMBER_ID))
                .join(AUTHORITIES)
                .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
                .leftJoin(MEMBER_TEAMS)
                .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .leftJoin(TEAMS)
                .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                .where(
                    GATHERING_MEMBERS.GATHERING_ID
                        .eq(gatheringId.value)
                        .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
                ).fetch()

        return queryResults.map { query ->

            val memberName = query.get(MEMBERS.NAME)
            val teamNumber = query.get(TEAMS.NUMBER)
            val memberPart = query.get(MEMBERS.PART)
            val authorityName = query.get(AUTHORITIES.NAME)
            val isInvitationSubmitted = query.get(GATHERING_MEMBERS.IS_INVITATION_SUBMITTED)

            if (memberName == null || authorityName == null || isInvitationSubmitted == null) {
                throw GatheringMemberNotFoundException()
            }
            BillMemberIsInvitationSubmittedQueryModel(
                name = memberName,
                teamNumber = teamNumber ?: 0,
                part = memberPart,
                authority = authorityName,
                isInvitationSubmitted = isInvitationSubmitted,
            )
        }
    }

    override fun findGatheringMembersByGatheringIdsAndMemberIds(
        gatheringIds: List<GatheringId>,
        memberIds: List<MemberId>,
    ): List<GatheringMember> {
        val queryResult =
            dsl
                .select(
                    GATHERING_MEMBERS.GATHERING_MEMBER_ID,
                    GATHERING_MEMBERS.MEMBER_ID,
                    GATHERING_MEMBERS.GATHERING_ID,
                    GATHERING_MEMBERS.IS_VIEWED,
                    GATHERING_MEMBERS.IS_JOINED,
                    GATHERING_MEMBERS.IS_INVITATION_SUBMITTED,
                    GATHERING_MEMBERS.CREATED_AT,
                    GATHERING_MEMBERS.COMPLETED_AT,
                    GATHERING_MEMBERS.UPDATED_AT,
                    GATHERING_MEMBERS.DELETED_AT,
                ).from(GATHERING_MEMBERS)
                .where(GATHERING_MEMBERS.MEMBER_ID.`in`(memberIds))
                .and(GATHERING_MEMBERS.GATHERING_ID.`in`(gatheringIds))
                .fetch()

        return queryResult.map { query ->
            val id = query.get(GATHERING_MEMBERS.GATHERING_MEMBER_ID)
            val memberId = query.get(GATHERING_MEMBERS.MEMBER_ID)
            val gatheringId = query.get(GATHERING_MEMBERS.GATHERING_ID)
            val isViewed = query.get(GATHERING_MEMBERS.IS_VIEWED)
            val isJoined = query.get(GATHERING_MEMBERS.IS_JOINED)
            val isInvitationSubmitted = query.get(GATHERING_MEMBERS.IS_INVITATION_SUBMITTED)
            val createdAt = query.get(GATHERING_MEMBERS.CREATED_AT)
            val completedAt = query.get(GATHERING_MEMBERS.COMPLETED_AT)
            val updatedAt = query.get(GATHERING_MEMBERS.UPDATED_AT)
            val deletedAt = query.get(GATHERING_MEMBERS.DELETED_AT)

            GatheringMember(
                id = GatheringMemberId(id ?: throw GatheringMemberNotFoundException()),
                memberId = MemberId(memberId ?: throw MemberNotFoundException()),
                gatheringId = GatheringId(gatheringId ?: throw GatheringNotFoundException()),
                isViewed = isViewed!!,
                isJoined = isJoined,
                isInvitationSubmitted = isInvitationSubmitted!!,
                createdAt = createdAt?.let { localDateTimeToInstant(it) },
                completedAt = completedAt?.let { localDateTimeToInstant(it) },
                updatedAt = updatedAt?.let { localDateTimeToInstant(it) },
                deletedAt = deletedAt?.let { localDateTimeToInstant(it) },
            )
        }
    }
}
