package com.server.dpmcore.member.memberTeam.domain

import com.server.dpmcore.member.member.domain.MemberId
import com.server.dpmcore.team.domain.TeamId

data class MemberTeam(
    val id: MemberTeamId,
    val memberId: MemberId,
    val teamId: TeamId
)
