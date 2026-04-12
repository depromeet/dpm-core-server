package core.persistence.member.repository.role

import core.domain.member.aggregate.MemberRole
import core.domain.member.port.outbound.MemberRolePersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.ROLES
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberRoleRepository(
    private val dsl: DSLContext,
) : MemberRolePersistencePort {
    override fun save(memberRole: MemberRole) {
        dsl
            .insertInto(MEMBER_ROLES)
            .set(MEMBER_ROLES.MEMBER_ID, memberRole.memberId.value)
            .set(MEMBER_ROLES.ROLE_ID, memberRole.roleId.value)
            .set(
                MEMBER_ROLES.GRANTED_AT,
                memberRole.grantedAt
                    ?.atZone(ZoneId.of(TIME_ZONE))
                    ?.toLocalDateTime()
                    ?: LocalDateTime.now(),
            ).execute()
    }

    override fun upsertSingleActiveRole(
        memberId: Long,
        roleId: Long,
    ) {
        val now = LocalDateTime.now(ZoneId.of(TIME_ZONE))
        val activeRoles =
            dsl
                .select(MEMBER_ROLES.MEMBER_ROLE_ID, MEMBER_ROLES.ROLE_ID)
                .from(MEMBER_ROLES)
                .where(MEMBER_ROLES.MEMBER_ID.eq(memberId))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .orderBy(MEMBER_ROLES.MEMBER_ROLE_ID.asc())
                .fetch()

        if (activeRoles.isEmpty()) {
            dsl
                .insertInto(MEMBER_ROLES)
                .set(MEMBER_ROLES.MEMBER_ID, memberId)
                .set(MEMBER_ROLES.ROLE_ID, roleId)
                .set(MEMBER_ROLES.GRANTED_AT, now)
                .execute()
            return
        }

        val keptRole =
            activeRoles.firstOrNull { it[MEMBER_ROLES.ROLE_ID] == roleId }
                ?: activeRoles.first()
        val keptRoleId = keptRole[MEMBER_ROLES.MEMBER_ROLE_ID] ?: return

        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.ROLE_ID, roleId)
            .set(MEMBER_ROLES.GRANTED_AT, now)
            .set(MEMBER_ROLES.DELETED_AT, null as LocalDateTime?)
            .where(MEMBER_ROLES.MEMBER_ROLE_ID.eq(keptRoleId))
            .execute()

        val duplicateRoleIds =
            activeRoles
                .mapNotNull { it[MEMBER_ROLES.MEMBER_ROLE_ID] }
                .filter { it != keptRoleId }

        if (duplicateRoleIds.isNotEmpty()) {
            dsl
                .update(MEMBER_ROLES)
                .set(MEMBER_ROLES.DELETED_AT, now)
                .where(MEMBER_ROLES.MEMBER_ROLE_ID.`in`(duplicateRoleIds))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .execute()
        }
    }

    override fun upsertCohortRole(
        memberId: Long,
        roleId: Long,
        cohortRolePrefix: String,
    ) {
        val now = LocalDateTime.now(ZoneId.of(TIME_ZONE))
        val activeCohortRoles =
            dsl
                .select(MEMBER_ROLES.MEMBER_ROLE_ID, MEMBER_ROLES.ROLE_ID)
                .from(MEMBER_ROLES)
                .join(ROLES)
                .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                .where(MEMBER_ROLES.MEMBER_ID.eq(memberId))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .and(ROLES.NAME.startsWith(cohortRolePrefix))
                .orderBy(MEMBER_ROLES.MEMBER_ROLE_ID.asc())
                .fetch()

        if (activeCohortRoles.isEmpty()) {
            dsl
                .insertInto(MEMBER_ROLES)
                .set(MEMBER_ROLES.MEMBER_ID, memberId)
                .set(MEMBER_ROLES.ROLE_ID, roleId)
                .set(MEMBER_ROLES.GRANTED_AT, now)
                .execute()
            return
        }

        val keptRole =
            activeCohortRoles.firstOrNull { it[MEMBER_ROLES.ROLE_ID] == roleId }
                ?: activeCohortRoles.first()
        val keptRoleId = keptRole[MEMBER_ROLES.MEMBER_ROLE_ID] ?: return

        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.ROLE_ID, roleId)
            .set(MEMBER_ROLES.GRANTED_AT, now)
            .set(MEMBER_ROLES.DELETED_AT, null as LocalDateTime?)
            .where(MEMBER_ROLES.MEMBER_ROLE_ID.eq(keptRoleId))
            .execute()

        val duplicateRoleIds =
            activeCohortRoles
                .mapNotNull { it[MEMBER_ROLES.MEMBER_ROLE_ID] }
                .filter { it != keptRoleId }

        if (duplicateRoleIds.isNotEmpty()) {
            dsl
                .update(MEMBER_ROLES)
                .set(MEMBER_ROLES.DELETED_AT, now)
                .where(MEMBER_ROLES.MEMBER_ROLE_ID.`in`(duplicateRoleIds))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .execute()
        }
    }

    /**
     * 단순 soft delete를 위해 jOOQ 사용.
     *
     * MemberAuthority의 연관관계가 복잡해 JPA로 save를 시도하면 불필요한 연관 엔티티 조회가 발생함.
     *
     * 이에 jOOQ로 직접 update 쿼리를 작성하여, 성능을 최적화하고 불필요한 JPA 연산을 최소화함.
     *
     * @author LeeHanEum
     * @since 2025.09.02
     */
    override fun softDeleteAllByMemberId(memberId: Long) {
        dsl
            .update(MEMBER_ROLES)
            .set(
                MEMBER_ROLES.DELETED_AT,
                LocalDateTime.now(ZoneId.of(TIME_ZONE)),
            ).where(
                MEMBER_ROLES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).execute()
    }

    override fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    ) {
        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.DELETED_AT, LocalDateTime.now(ZoneId.of(TIME_ZONE)))
            .where(
                MEMBER_ROLES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_ROLES.ROLE_ID.eq(roleId))
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).execute()
    }

    override fun findRoleNamesByMemberId(memberId: Long): List<String> =
        dsl
            .select(ROLES.NAME)
            .from(MEMBER_ROLES)
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(
                MEMBER_ROLES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).fetch(ROLES.NAME)
            .filterNotNull()

    override fun findRoleNamesByMemberIds(memberIds: List<Long>): Map<Long, List<String>> {
        if (memberIds.isEmpty()) {
            return emptyMap()
        }

        return dsl
            .select(MEMBER_ROLES.MEMBER_ID, ROLES.NAME)
            .from(MEMBER_ROLES)
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(
                MEMBER_ROLES.MEMBER_ID
                    .`in`(memberIds)
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).fetch()
            .mapNotNull { record ->
                val memberId = record[MEMBER_ROLES.MEMBER_ID] ?: return@mapNotNull null
                val roleName = record[ROLES.NAME] ?: return@mapNotNull null
                memberId to roleName
            }.groupBy(
                keySelector = { (memberId, _) -> memberId },
                valueTransform = { (_, roleName) -> roleName },
            )
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}
