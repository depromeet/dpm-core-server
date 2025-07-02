package com.server.dpmcore.member.memberTeam.domain

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.team.domain.model.TeamId

data class MemberTeam(
    val id: MemberTeamId = MemberTeamId.generate(),
    val memberId: MemberId,
    val teamId: TeamId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberTeam

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
