package com.server.dpmcore.member.memberTeam.application

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberTeam.domain.model.MemberTeam
import com.server.dpmcore.member.memberTeam.domain.port.outbound.MemberTeamPersistencePort
import com.server.dpmcore.team.domain.model.TeamId
import org.springframework.stereotype.Service

@Service
class MemberTeamService(
    private val memberTeamPersistencePort: MemberTeamPersistencePort,
) {
    fun addMemberToTeam(
        memberId: MemberId,
        teamId: TeamId,
    ) {
        memberTeamPersistencePort.save(MemberTeam.of(memberId, teamId))
    }
}
