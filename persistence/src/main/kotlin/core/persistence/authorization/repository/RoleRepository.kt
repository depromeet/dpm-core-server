package core.persistence.authorization.repository

import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.member.vo.MemberId
import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.jooq.dsl.tables.references.MEMBER_PERMISSIONS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.PERMISSIONS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.ROLE_PERMISSIONS
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RoleRepository(
    private val roleJpaRepository: RoleJpaRepository,
    private val dsl: DSLContext,
) : RolePersistencePort {
    override fun findAll(): List<Role> = roleJpaRepository.findAll().mapNotNull { it.toDomain() }

    override fun findAllByMemberExternalId(externalId: String): List<String> =
        run {
            val latestMemberCohorts = latestMemberCohorts()

            dsl
            .select(ROLES.NAME)
            .from(ROLES)
            .join(MEMBER_ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .join(MEMBERS)
            .on(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_OAUTH)
            .on(MEMBER_OAUTH.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(latestMemberCohorts)
            .on(latestMemberCohortMemberIdField(latestMemberCohorts).eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_COHORT_ID.eq(latestMemberCohortIdField(latestMemberCohorts)))
            .join(COHORTS)
            .on(COHORTS.COHORT_ID.eq(MEMBER_COHORTS.COHORT_ID))
            .where(MEMBER_OAUTH.EXTERNAL_ID.eq(externalId))
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .and(MEMBERS.DELETED_AT.isNull)
            .and(ROLES.NAME.like(COHORTS.VALUE.concat(inline("기 %"))))
            .fetch(ROLES.NAME)
            .filterNotNull()
        }

    override fun findAllPermissionsByMemberId(memberId: MemberId): List<String> =
        findAllPermissionsByMemberIdAndRoleNames(
            memberId = memberId,
            roleNames = findCurrentRoleNamesByMemberId(memberId.value),
        )

    override fun findAllPermissionsByMemberIdAndRoleNames(
        memberId: MemberId,
        roleNames: List<String>,
    ): List<String> {
        val currentRoleNames = findCurrentRoleNamesByMemberId(memberId.value)
        val filteredRoleNames = if (roleNames.isEmpty()) emptyList() else currentRoleNames.filter { it in roleNames }

        val directPermissionQuery =
            dsl
                .select(PERMISSIONS.RESOURCE, PERMISSIONS.ACTION)
                .from(PERMISSIONS)
                .join(MEMBER_PERMISSIONS)
                .on(PERMISSIONS.PERMISSION_ID.eq(MEMBER_PERMISSIONS.PERMISSION_ID))
                .where(MEMBER_PERMISSIONS.MEMBER_ID.eq(memberId.value))
                .and(MEMBER_PERMISSIONS.DELETED_AT.isNull)

        if (filteredRoleNames.isEmpty()) {
            return directPermissionQuery
                .fetch()
                .map { "${it.get(PERMISSIONS.ACTION)}:${it.get(PERMISSIONS.RESOURCE)}".lowercase() }
        }

        return directPermissionQuery
            .union(
                dsl.select(PERMISSIONS.RESOURCE, PERMISSIONS.ACTION)
                    .from(PERMISSIONS)
                    .join(ROLE_PERMISSIONS)
                    .on(PERMISSIONS.PERMISSION_ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                    .join(MEMBER_ROLES)
                    .on(ROLE_PERMISSIONS.ROLE_ID.eq(MEMBER_ROLES.ROLE_ID))
                    .join(ROLES)
                    .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                    .where(MEMBER_ROLES.MEMBER_ID.eq(memberId.value))
                    .and(MEMBER_ROLES.DELETED_AT.isNull)
                    .and(ROLE_PERMISSIONS.REVOKED_AT.isNull)
                    .and(ROLES.NAME.`in`(filteredRoleNames)),
            ).fetch()
            .map { "${it.get(PERMISSIONS.ACTION)}:${it.get(PERMISSIONS.RESOURCE)}".lowercase() }
    }

    override fun findIdByName(roleName: String): Long =
        dsl
            .select(ROLES.ROLE_ID)
            .from(ROLES)
            .where(ROLES.NAME.eq(roleName))
            .fetchOne(ROLES.ROLE_ID)
            ?: throw IllegalArgumentException("Role not found: $roleName")

    override fun createRoleWithPermissions(
        roleName: String,
        permissionIds: List<Long>,
    ): Long {
        val roleId =
            dsl
                .insertInto(ROLES)
                .set(ROLES.NAME, roleName)
                .returning(ROLES.ROLE_ID)
                .fetchOne(ROLES.ROLE_ID)
                ?: throw IllegalStateException("Failed to create role: $roleName")

        if (permissionIds.isNotEmpty()) {
            val now = LocalDateTime.now()
            dsl.batch(
                permissionIds.map { permissionId ->
                    dsl
                        .insertInto(ROLE_PERMISSIONS)
                        .set(ROLE_PERMISSIONS.ROLE_ID, roleId)
                        .set(ROLE_PERMISSIONS.PERMISSION_ID, permissionId)
                        .set(ROLE_PERMISSIONS.GRANTED_AT, now)
                },
            ).execute()
        }
        return roleId
    }

    override fun findPermissionIdsByRoleName(roleName: String): List<Long> =
        dsl
            .select(ROLE_PERMISSIONS.PERMISSION_ID)
            .from(ROLE_PERMISSIONS)
            .join(ROLES)
            .on(ROLE_PERMISSIONS.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(ROLES.NAME.eq(roleName))
            .and(ROLE_PERMISSIONS.REVOKED_AT.isNull)
            .fetch(ROLE_PERMISSIONS.PERMISSION_ID)
            .filterNotNull()

    override fun existsByName(roleName: String): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(ROLES)
                .where(ROLES.NAME.eq(roleName)),
        )

    private fun findCurrentRoleNamesByMemberId(memberId: Long): List<String> {
        val latestMemberCohorts = latestMemberCohorts()
        return dsl
            .select(ROLES.NAME)
            .from(ROLES)
            .join(MEMBER_ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .join(MEMBERS)
            .on(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(latestMemberCohorts)
            .on(latestMemberCohortMemberIdField(latestMemberCohorts).eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_COHORT_ID.eq(latestMemberCohortIdField(latestMemberCohorts)))
            .join(COHORTS)
            .on(COHORTS.COHORT_ID.eq(MEMBER_COHORTS.COHORT_ID))
            .where(MEMBER_ROLES.MEMBER_ID.eq(memberId))
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .and(MEMBERS.DELETED_AT.isNull)
            .and(ROLES.NAME.like(COHORTS.VALUE.concat(inline("기 %"))))
            .fetch(ROLES.NAME)
            .filterNotNull()
    }

    private fun latestMemberCohorts() =
        dsl
            .select(
                MEMBER_COHORTS.MEMBER_ID,
                max(MEMBER_COHORTS.MEMBER_COHORT_ID).`as`("latest_member_cohort_id"),
            )
            .from(MEMBER_COHORTS)
            .groupBy(MEMBER_COHORTS.MEMBER_ID)
            .asTable("latest_member_cohorts")

    private fun latestMemberCohortIdField(table: Table<*>) =
        table.field(name("latest_member_cohort_id"), Long::class.java)!!

    private fun latestMemberCohortMemberIdField(table: Table<*>) =
        table.field(MEMBER_COHORTS.MEMBER_ID)!!
}
