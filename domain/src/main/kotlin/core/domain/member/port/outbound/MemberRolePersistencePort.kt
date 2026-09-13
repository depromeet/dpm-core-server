package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberRole
import core.domain.member.vo.MemberRoleAssignment

interface MemberRolePersistencePort {
    fun save(memberRole: MemberRole)

    fun upsertSingleActiveRole(
        memberId: Long,
        roleId: Long,
        cohortId: Long? = null,
    )

    fun replaceCohortRole(
        memberId: Long,
        roleId: Long,
        cohortId: Long,
    )

    fun findRoleNamesByMemberId(memberId: Long): List<String>

    fun findActiveRoleAssignmentsByMemberId(memberId: Long): List<MemberRoleAssignment>

    fun findRoleNamesByMemberIds(memberIds: List<Long>): Map<Long, List<String>>

    fun softDeleteAllByMemberId(memberId: Long)

    fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    )
}
