package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberRole

interface MemberRolePersistencePort {
    fun save(memberRole: MemberRole)

    fun upsertSingleActiveRole(
        memberId: Long,
        roleId: Long,
    )

    fun upsertCohortRole(
        memberId: Long,
        roleId: Long,
        cohortRolePrefix: String,
    )

    fun findRoleNamesByMemberId(memberId: Long): List<String>

    fun findRoleNamesByMemberIds(memberIds: List<Long>): Map<Long, List<String>>

    fun softDeleteAllByMemberId(memberId: Long)

    fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    )
}
