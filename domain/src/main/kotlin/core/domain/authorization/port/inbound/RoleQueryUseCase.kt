package core.domain.authorization.port.inbound

import core.domain.authorization.aggregate.Role
import core.domain.authorization.vo.RoleId
import core.domain.member.vo.MemberId

interface RoleQueryUseCase {
    fun getAllRoles(): List<Role>

    fun getRolesByExternalId(externalId: String): List<String>

    fun getPermissionsByMemberId(memberId: MemberId): List<String>

    fun findIdByName(roleName: String): Long
}
