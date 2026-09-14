package core.persistence.member.repository.role

import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberRole
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberRoleAssignment
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.ROLES
import org.jooq.impl.DSL
import org.jooq.impl.DSL.name
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
            .set(COHORT_ID_FIELD, memberRole.cohortId?.value)
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
        cohortId: Long?,
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
                .set(COHORT_ID_FIELD, cohortId)
                .set(MEMBER_ROLES.GRANTED_AT, now)
                .execute()
            return
        }

        val keptRole = activeRoles.firstOrNull { it[MEMBER_ROLES.ROLE_ID] == roleId } ?: activeRoles.first()
        val keptRoleId = keptRole[MEMBER_ROLES.MEMBER_ROLE_ID] ?: return

        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.ROLE_ID, roleId)
            .set(COHORT_ID_FIELD, cohortId)
            .set(MEMBER_ROLES.GRANTED_AT, now)
            .set(MEMBER_ROLES.DELETED_AT, null as LocalDateTime?)
            .where(MEMBER_ROLES.MEMBER_ROLE_ID.eq(keptRoleId))
            .execute()

        softDeleteDuplicates(
            activeRoles.mapNotNull { it[MEMBER_ROLES.MEMBER_ROLE_ID] }.filter { it != keptRoleId },
            now,
        )
    }

    override fun replaceCohortRole(
        memberId: Long,
        roleId: Long,
        cohortId: Long,
    ) {
        val now = LocalDateTime.now(ZoneId.of(TIME_ZONE))
        val cohortBoundRoleNames = listOf("ORGANIZER", "DEEPER")
        val activeRoles =
            dsl
                .select(MEMBER_ROLES.MEMBER_ROLE_ID, MEMBER_ROLES.ROLE_ID, COHORT_ID_FIELD)
                .from(MEMBER_ROLES)
                .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                .where(MEMBER_ROLES.MEMBER_ID.eq(memberId))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .and(ROLES.NAME.`in`(cohortBoundRoleNames))
                .and(COHORT_ID_FIELD.eq(cohortId))
                .orderBy(MEMBER_ROLES.MEMBER_ROLE_ID.asc())
                .fetch()

        if (activeRoles.isEmpty()) {
            dsl
                .insertInto(MEMBER_ROLES)
                .set(MEMBER_ROLES.MEMBER_ID, memberId)
                .set(MEMBER_ROLES.ROLE_ID, roleId)
                .set(COHORT_ID_FIELD, cohortId)
                .set(MEMBER_ROLES.GRANTED_AT, now)
                .execute()
            return
        }

        val keptRoleId = activeRoles.first()[MEMBER_ROLES.MEMBER_ROLE_ID] ?: return
        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.ROLE_ID, roleId)
            .set(COHORT_ID_FIELD, cohortId)
            .set(MEMBER_ROLES.GRANTED_AT, now)
            .set(MEMBER_ROLES.DELETED_AT, null as LocalDateTime?)
            .where(MEMBER_ROLES.MEMBER_ROLE_ID.eq(keptRoleId))
            .execute()

        softDeleteDuplicates(
            activeRoles.mapNotNull { it[MEMBER_ROLES.MEMBER_ROLE_ID] }.filter { it != keptRoleId },
            now,
        )
    }

    override fun softDeleteAllByMemberId(memberId: Long) {
        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.DELETED_AT, LocalDateTime.now(ZoneId.of(TIME_ZONE)))
            .where(MEMBER_ROLES.MEMBER_ID.eq(memberId).and(MEMBER_ROLES.DELETED_AT.isNull))
            .execute()
    }

    override fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    ) {
        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.DELETED_AT, LocalDateTime.now(ZoneId.of(TIME_ZONE)))
            .where(
                MEMBER_ROLES.MEMBER_ID.eq(memberId)
                    .and(MEMBER_ROLES.ROLE_ID.eq(roleId))
                    .and(MEMBER_ROLES.DELETED_AT.isNull),
            ).execute()
    }

    override fun findRoleNamesByMemberId(memberId: Long): List<String> =
        findActiveRoleAssignmentsByMemberId(memberId).map { it.roleName }

    override fun findActiveRoleAssignmentsByMemberId(memberId: Long): List<MemberRoleAssignment> =
        dsl
            .select(ROLES.NAME, COHORT_ID_FIELD)
            .from(MEMBER_ROLES)
            .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(MEMBER_ROLES.MEMBER_ID.eq(memberId).and(MEMBER_ROLES.DELETED_AT.isNull))
            .fetch()
            .mapNotNull { record ->
                val roleName = record.get(ROLES.NAME) ?: return@mapNotNull null
                val cohortIdValue = record.get(COHORT_ID_FIELD)
                MemberRoleAssignment(
                    roleName = roleName,
                    cohortId = cohortIdValue?.let { CohortId(it) },
                )
            }

    override fun findRoleNamesByMemberIds(memberIds: List<Long>): Map<Long, List<String>> {
        if (memberIds.isEmpty()) return emptyMap()
        return dsl
            .select(MEMBER_ROLES.MEMBER_ID, ROLES.NAME)
            .from(MEMBER_ROLES)
            .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(MEMBER_ROLES.MEMBER_ID.`in`(memberIds).and(MEMBER_ROLES.DELETED_AT.isNull))
            .fetch()
            .mapNotNull { record ->
                val memberId = record[MEMBER_ROLES.MEMBER_ID] ?: return@mapNotNull null
                val roleName = record[ROLES.NAME] ?: return@mapNotNull null
                memberId to roleName
            }.groupBy({ it.first }, { it.second })
    }

    private fun softDeleteDuplicates(
        roleIds: List<Long>,
        now: LocalDateTime,
    ) {
        if (roleIds.isEmpty()) return
        dsl
            .update(MEMBER_ROLES)
            .set(MEMBER_ROLES.DELETED_AT, now)
            .where(MEMBER_ROLES.MEMBER_ROLE_ID.`in`(roleIds).and(MEMBER_ROLES.DELETED_AT.isNull))
            .execute()
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
        private val COHORT_ID_FIELD = DSL.field(name("member_roles", "cohort_id"), Long::class.java)
    }
}
