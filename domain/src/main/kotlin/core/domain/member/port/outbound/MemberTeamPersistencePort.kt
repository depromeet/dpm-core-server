package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberTeam

interface MemberTeamPersistencePort {
    fun save(memberTeam: MemberTeam)

    fun deleteByMemberId(memberId: Long)

    /** 현재 기수의 팀 연결만 교체한다. null은 미배정이며 실제 변경한 멤버 ID를 반환한다. */
    fun replaceCurrentCohortTeams(
        memberIds: List<Long>,
        cohortId: Long,
        teamId: Long?,
    ): Set<Long>
}
