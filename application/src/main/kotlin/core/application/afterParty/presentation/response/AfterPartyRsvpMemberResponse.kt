package core.application.afterParty.presentation.response

import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber

data class AfterPartyRsvpMemberResponse(
    val memberId: MemberId,
    val name: String,
    val part: MemberPart?,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val rsvpStatus: Boolean?,
)
