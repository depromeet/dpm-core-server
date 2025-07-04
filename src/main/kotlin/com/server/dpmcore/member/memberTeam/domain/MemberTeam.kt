package com.server.dpmcore.member.memberTeam.domain

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.team.domain.model.TeamId

class MemberTeam(
    val id: MemberTeamId? = null,
    val memberId: MemberId,
    val teamId: TeamId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberTeam) return false

        return id == other.id &&
            memberId == other.memberId &&
            teamId == other.teamId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + teamId.hashCode()
        return result
    }

    override fun toString(): String {
        return "MemberTeam(id=$id, memberId=$memberId, teamId=$teamId)"
    }


}
