package core.domain.member.aggregate

import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberTeamId
import core.domain.team.vo.TeamId

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

    override fun toString(): String = "MemberTeam(id=$id, memberId=$memberId, teamId=$teamId)"

    companion object {
        fun of(
            memberId: MemberId,
            teamId: TeamId,
        ): MemberTeam =
            MemberTeam(
                memberId = memberId,
                teamId = teamId,
            )
    }
}
