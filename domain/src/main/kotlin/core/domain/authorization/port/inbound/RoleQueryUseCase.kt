package core.domain.authorization.port.inbound

import core.domain.authorization.aggregate.Role
import core.domain.member.vo.MemberId

interface RoleQueryUseCase {
    fun getAllRoles(): List<Role>

    fun getRolesByCohort(cohort: String): List<Role>

    fun getRoleNamesByMemberId(memberId: MemberId): List<String>

    fun getRoleNamesByMemberIds(memberIds: List<MemberId>): Map<MemberId, List<String>>

    fun getRolesByExternalId(externalId: String): List<String>

    fun getPermissionsByMemberId(memberId: MemberId): List<String>

    fun findIdByName(roleName: String): Long
}
