package core.application.gathering.presentation.response

import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber

data class GatheringV2RsvpMemberResponse(
    val memberId: MemberId,
    val name: String,
    val part: MemberPart?,
    val teamNumber: TeamNumber,
    val rsvpStatus: Boolean?,
)
