package core.application.notification.presentation.request

import core.domain.member.vo.MemberId

data class NotificationRequest(
    val targetMemberId: MemberId,
    val title: String,
    val message: String,
)
