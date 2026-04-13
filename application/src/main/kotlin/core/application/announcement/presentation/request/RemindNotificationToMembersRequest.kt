package core.application.announcement.presentation.request

import core.domain.member.vo.MemberId

data class RemindNotificationToMembersRequest(
    val memberIds: List<MemberId>,
)
