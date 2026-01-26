package core.application.gathering.presentation.response

import core.domain.member.vo.MemberId

data class GatheringV2ParticipantMemberDetailResponse(
    val memberId: MemberId,
    val name: String,
    val profileUrl: String,
    val part: String,
    val team: String,
    val isJoined: Boolean,
)
