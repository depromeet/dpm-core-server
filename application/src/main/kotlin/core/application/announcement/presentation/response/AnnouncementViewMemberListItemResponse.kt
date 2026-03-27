package core.application.announcement.presentation.response

import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber

data class AnnouncementViewMemberListItemResponse(
    val memberId: MemberId,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: MemberPart?,
) {
    companion object {
        fun of(
            member: Member,
            teamNumber: TeamNumber,
            isAdmin: Boolean,
        ): AnnouncementViewMemberListItemResponse =
            AnnouncementViewMemberListItemResponse(
                memberId = member.id!!,
                name = member.name,
                teamNumber = teamNumber,
                isAdmin = isAdmin,
                part = member.part,
            )
    }
}
