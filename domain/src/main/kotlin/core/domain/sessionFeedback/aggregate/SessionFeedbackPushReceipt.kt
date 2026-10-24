package core.domain.sessionFeedback.aggregate

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.vo.SessionFeedbackPushReceiptId
import java.time.Instant

class SessionFeedbackPushReceipt(
    val id: SessionFeedbackPushReceiptId? = null,
    val sessionId: SessionId,
    val memberId: MemberId,
    val sentAt: Instant,
) {
    companion object {
        fun create(
            sessionId: SessionId,
            memberId: MemberId,
            sentAt: Instant,
        ): SessionFeedbackPushReceipt =
            SessionFeedbackPushReceipt(
                sessionId = sessionId,
                memberId = memberId,
                sentAt = sentAt,
            )
    }
}
