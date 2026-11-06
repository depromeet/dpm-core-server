package core.domain.notification.event

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class AbsenceReasonReviewedEvent(
    val sessionId: SessionId,
    val memberId: MemberId,
    val week: Int,
    val approved: Boolean,
)
