package core.application.announcement.presentation.request

import core.domain.announcement.enums.SubmitStatus
import core.domain.member.vo.MemberId

data class UpdateSubmitStatusRequest(
    val submitStatus: SubmitStatus,
    val memberIds: List<MemberId>,
)
