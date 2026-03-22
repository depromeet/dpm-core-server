package core.domain.authorization.port.outbound

import core.domain.authorization.aggregate.Role
import core.domain.member.vo.MemberId

interface RolePersistencePort {
    fun findAll(): List<Role>

    fun findAllByMemberExternalId(externalId: String): List<String>

    fun findAllPermissionsByMemberId(memberId: MemberId): List<String>

    fun findAllPermissionsByMemberIdAndRoleNames(
        memberId: MemberId,
        roleNames: List<String>,
    ): List<String>

    fun findIdByName(roleName: String): Long

    fun createRoleWithPermissions(
        roleName: String,
        permissionIds: List<Long>,
    ): Long

    fun findPermissionIdsByRoleName(roleName: String): List<Long>

    fun existsByName(roleName: String): Boolean
}
