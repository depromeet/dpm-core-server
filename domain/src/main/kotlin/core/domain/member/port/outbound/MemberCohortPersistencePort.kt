package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberCohort

interface MemberCohortPersistencePort {
    fun save(memberCohort: MemberCohort)

    fun existsByMemberIdAndCohortId(
        memberId: Long,
        cohortId: Long,
    ): Boolean

    fun deleteByMemberId(memberId: Long)

    fun findCohortIdsByMemberId(memberId: Long): List<Long>

    fun findCohortIdsByMemberIds(memberIds: List<Long>): Map<Long, List<Long>>
}
