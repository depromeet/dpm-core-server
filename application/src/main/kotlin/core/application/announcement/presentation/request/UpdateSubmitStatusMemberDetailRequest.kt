package core.application.announcement.presentation.request

import core.domain.member.vo.MemberId

data class UpdateSubmitStatusMemberDetailRequest(
    val memberId: MemberId,
    val score: Int?,
)
