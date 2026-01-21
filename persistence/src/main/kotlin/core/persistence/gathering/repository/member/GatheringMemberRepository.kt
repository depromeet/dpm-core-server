package core.persistence.gathering.repository.member

import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.port.outbound.GatheringMemberPersistencePort
import core.domain.gathering.port.outbound.query.GatheringMemberIsJoinQueryModel
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringMemberId
import core.domain.member.vo.MemberId
import core.entity.gathering.GatheringMemberEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.GATHERING_MEMBERS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.TEAMS
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

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
    ): GatheringMember? = gatheringJpaRepository.findByGatheringIdAndMemberId(gatheringId, memberId)?.toDomain()

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
        dsl.select(GATHERING_MEMBERS.MEMBER_ID)
            .from(GATHERING_MEMBERS)
            .where(GATHERING_MEMBERS.GATHERING_ID.eq(gatheringId.value))
            .fetch(GATHERING_MEMBERS.MEMBER_ID)
            .mapNotNull { it?.let { MemberId(it) } }

    override fun findGatheringMemberWithIsJoinByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<GatheringMemberIsJoinQueryModel> =
        dsl.select(
            MEMBERS.NAME,
            TEAMS.NUMBER,
            MEMBERS.PART,
            ROLES.NAME,
            GATHERING_MEMBERS.IS_JOINED,
        )
            .from(GATHERING_MEMBERS)
            .join(MEMBERS).on(GATHERING_MEMBERS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_ROLES).on(MEMBERS.MEMBER_ID.eq(MEMBER_ROLES.MEMBER_ID))
            .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .leftJoin(MEMBER_TEAMS).on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .leftJoin(TEAMS).on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                GATHERING_MEMBERS.GATHERING_ID.eq(gatheringId.value)
                    .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
            )
            .fetch()
            .mapNotNull { record ->
                val name = record.get(MEMBERS.NAME)
                val authority = record.get(ROLES.NAME)
                val isJoined = record.get(GATHERING_MEMBERS.IS_JOINED)

                if (name != null && authority != null && isJoined != null) {
                    GatheringMemberIsJoinQueryModel(
                        name = name,
                        teamNumber = record.get(TEAMS.NUMBER) ?: 0,
                        part = record.get(MEMBERS.PART),
                        authority = authority,
                        isJoined = isJoined,
                    )
                } else {
                    null
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
    ): List<BillMemberIsInvitationSubmittedQueryModel> =
        dsl.select(
            MEMBERS.NAME,
            TEAMS.NUMBER,
            MEMBERS.PART,
            ROLES.NAME,
            GATHERING_MEMBERS.IS_INVITATION_SUBMITTED,
        )
            .from(GATHERING_MEMBERS)
            .join(MEMBERS).on(GATHERING_MEMBERS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_ROLES).on(MEMBERS.MEMBER_ID.eq(MEMBER_ROLES.MEMBER_ID))
            .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .leftJoin(MEMBER_TEAMS).on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .leftJoin(TEAMS).on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                GATHERING_MEMBERS.GATHERING_ID.eq(gatheringId.value)
                    .and(GATHERING_MEMBERS.MEMBER_ID.eq(memberId.value)),
            )
            .fetch()
            .mapNotNull { record ->
                val name = record.get(MEMBERS.NAME)
                val authority = record.get(ROLES.NAME)
                val isSubmitted = record.get(GATHERING_MEMBERS.IS_INVITATION_SUBMITTED)

                if (name != null && authority != null && isSubmitted != null) {
                    BillMemberIsInvitationSubmittedQueryModel(
                        name = name,
                        teamNumber = record.get(TEAMS.NUMBER) ?: 0,
                        part = record.get(MEMBERS.PART),
                        authority = authority,
                        isInvitationSubmitted = isSubmitted,
                    )
                } else {
                    null
                }
            }

    override fun findGatheringMembersByGatheringIdsAndMemberIds(
        gatheringIds: List<GatheringId>,
        memberIds: List<MemberId>,
    ): List<GatheringMember> =
        dsl.select(
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
        )
            .from(GATHERING_MEMBERS)
            .where(GATHERING_MEMBERS.MEMBER_ID.`in`(memberIds))
            .and(GATHERING_MEMBERS.GATHERING_ID.`in`(gatheringIds))
            .fetch()
            .mapNotNull { record ->
                val id = record.get(GATHERING_MEMBERS.GATHERING_MEMBER_ID)
                val memberId = record.get(GATHERING_MEMBERS.MEMBER_ID)
                val gatheringId = record.get(GATHERING_MEMBERS.GATHERING_ID)
                val isViewed = record.get(GATHERING_MEMBERS.IS_VIEWED)
                val isJoined = record.get(GATHERING_MEMBERS.IS_JOINED)
                val isInvitationSubmitted = record.get(GATHERING_MEMBERS.IS_INVITATION_SUBMITTED)

                if (id != null && memberId != null && gatheringId != null &&
                    isViewed != null && isInvitationSubmitted != null
                ) {
                    GatheringMember(
                        id = GatheringMemberId(id),
                        memberId = MemberId(memberId),
                        gatheringId = GatheringId(gatheringId),
                        isViewed = isViewed,
                        isJoined = isJoined,
                        isInvitationSubmitted = isInvitationSubmitted,
                        createdAt =
                            record.get(GATHERING_MEMBERS.CREATED_AT)?.atZone(ZoneId.of("Asia/Seoul"))
                                ?.toInstant(),
                        completedAt =
                            record.get(GATHERING_MEMBERS.COMPLETED_AT)?.atZone(ZoneId.of("Asia/Seoul"))
                                ?.toInstant(),
                        updatedAt =
                            record.get(GATHERING_MEMBERS.UPDATED_AT)?.atZone(ZoneId.of("Asia/Seoul"))
                                ?.toInstant(),
                        deletedAt =
                            record.get(GATHERING_MEMBERS.DELETED_AT)?.atZone(ZoneId.of("Asia/Seoul"))
                                ?.toInstant(),
                    )
                } else {
                    null
                }
            }
}
