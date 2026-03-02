package core.application.afterParty.presentation.response

import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId

data class AfterPartyRsvpMemberResponse(
    val memberId: MemberId,
    val name: String,
    val part: MemberPart?,
    val team: Int,
    val rsvpStatus: Boolean?,
)
