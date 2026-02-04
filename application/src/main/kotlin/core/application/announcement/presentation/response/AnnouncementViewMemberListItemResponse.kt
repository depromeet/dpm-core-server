package core.application.announcement.presentation.response

import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId

data class AnnouncementViewMemberListItemResponse(
    val memberId: MemberId,
    val name: String,
    val teamId: TeamId,
    val part: MemberPart?,
) {
    companion object {
        fun of(
            member: Member,
            teamId: TeamId,
        ): AnnouncementViewMemberListItemResponse =
            AnnouncementViewMemberListItemResponse(
                memberId = member.id!!,
                name = member.name,
                teamId = teamId,
                part = member.part,
            )
    }
}
