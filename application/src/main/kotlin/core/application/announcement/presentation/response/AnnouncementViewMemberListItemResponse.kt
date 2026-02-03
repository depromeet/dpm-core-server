package core.application.announcement.presentation.response

import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId

data class AnnouncementViewMemberListItemResponse(
    val memberId: MemberId,
    val name: String,
    val teamId: TeamId,
    val part: String,
    val profileUrl: String,
)
