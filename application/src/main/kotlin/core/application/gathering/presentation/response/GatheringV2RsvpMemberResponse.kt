package core.application.gathering.presentation.response

import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId

data class GatheringV2RsvpMemberResponse(
    val memberId: MemberId,
    val name: String,
    val part: MemberPart?,
    val team: Int,
    val isRsvpGoing: Boolean,
)
