package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberRole
import core.domain.member.vo.MemberRoleAssignment

interface MemberRolePersistencePort {
    fun save(memberRole: MemberRole)

    /** 현재 기수와 기수 없는 타입 역할만 교체한다. null은 미배정이며 실제 변경한 멤버 ID를 반환한다. */
    fun replaceCurrentCohortRoles(
        memberIds: List<Long>,
        cohortId: Long,
        roleId: Long?,
    ): Set<Long>

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

    fun findActiveRoleAssignmentsByMemberIds(memberIds: List<Long>): Map<Long, List<MemberRoleAssignment>>

    fun findRoleNamesByMemberIds(memberIds: List<Long>): Map<Long, List<String>>

    fun softDeleteAllByMemberId(memberId: Long)

    fun softDeleteByMemberIdAndRoleId(
        memberId: Long,
        roleId: Long,
    )
}
