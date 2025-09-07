package com.server.dpmcore.member.memberTeam.domain.port.outbound

import com.server.dpmcore.member.memberTeam.domain.model.MemberTeam

interface MemberTeamPersistencePort {
    fun save(memberTeam: MemberTeam)

    fun deleteByMemberId(memberId: Long)
}
