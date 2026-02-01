package core.domain.authorization.port.outbound

import core.domain.authorization.aggregate.Role
import core.domain.authorization.vo.RoleId
import core.domain.member.vo.MemberId

interface RolePersistencePort {
    fun findAll(): List<Role>

    fun findAllByMemberExternalId(externalId: String): List<String>

    fun findAllPermissionsByMemberId(memberId: MemberId): List<String>

    fun findIdByName(roleName: String): Long
}
