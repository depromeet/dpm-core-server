package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberTeam

interface MemberTeamPersistencePort {
    fun save(memberTeam: MemberTeam)

    fun deleteByMemberId(memberId: Long)
}
