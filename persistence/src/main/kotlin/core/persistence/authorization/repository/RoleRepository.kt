package core.persistence.authorization.repository

import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.member.vo.MemberId
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.jooq.dsl.tables.references.MEMBER_PERMISSIONS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.PERMISSIONS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.ROLE_PERMISSIONS
import org.springframework.stereotype.Repository

@Repository
class RoleRepository (
    private val roleJpaRepository: RoleJpaRepository,
    private val dsl: DSLContext,
) : RolePersistencePort {

    override fun findAll(): List<Role> {
        return roleJpaRepository.findAll().mapNotNull { it.toDomain() }
    }

    override fun findAllByMemberExternalId(externalId: String): List<String> =
        dsl
            .select(ROLES.NAME)
            .from(ROLES)
            .join(MEMBER_ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .join(MEMBERS)
            .on(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_OAUTH)
            .on(MEMBER_OAUTH.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(MEMBER_OAUTH.EXTERNAL_ID.eq(externalId).and(MEMBER_ROLES.DELETED_AT.isNull))
            .fetch(ROLES.NAME)
            .filterNotNull()

    override fun findAllPermissionsByMemberId(memberId: MemberId): List<String> =
        dsl
            .select(PERMISSIONS.RESOURCE, PERMISSIONS.ACTION) // 직접 할당 받은 권한 조회
            .from(PERMISSIONS)
            .join(MEMBER_PERMISSIONS)
            .on(PERMISSIONS.PERMISSION_ID.eq(MEMBER_PERMISSIONS.PERMISSION_ID))
            .where(MEMBER_PERMISSIONS.MEMBER_ID.eq(memberId.value))
            .union(
                dsl.select(PERMISSIONS.RESOURCE, PERMISSIONS.ACTION) // 역할을 통해 할당 받은 권한 조회
                    .from(PERMISSIONS)
                    .join(ROLE_PERMISSIONS)
                    .on(PERMISSIONS.PERMISSION_ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                    .join(MEMBER_ROLES)
                    .on(ROLE_PERMISSIONS.ROLE_ID.eq(MEMBER_ROLES.ROLE_ID))
                    .where(MEMBER_ROLES.MEMBER_ID.eq(memberId.value))
            )
            .fetch()
            .map { "${it.get(PERMISSIONS.ACTION)}:${it.get(PERMISSIONS.RESOURCE)}".lowercase() }

}
