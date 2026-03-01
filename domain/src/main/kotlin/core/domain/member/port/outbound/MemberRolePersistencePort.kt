package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberRole

interface MemberRolePersistencePort {
    fun save(memberRole: MemberRole)

    fun findRoleNamesByMemberId(memberId: Long): List<String>

    fun softDeleteAllByMemberId(memberId: Long)

    fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    )
}
