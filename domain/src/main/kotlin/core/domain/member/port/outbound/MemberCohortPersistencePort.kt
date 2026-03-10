package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberCohort

interface MemberCohortPersistencePort {
    fun save(memberCohort: MemberCohort)

    fun existsByMemberIdAndCohortId(
        memberId: Long,
        cohortId: Long,
    ): Boolean

    fun deleteByMemberId(memberId: Long)
}
