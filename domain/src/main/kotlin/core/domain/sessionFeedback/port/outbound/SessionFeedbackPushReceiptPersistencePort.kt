package core.domain.sessionFeedback.port.outbound

import core.domain.member.vo.MemberId
import core.domain.sessionFeedback.aggregate.SessionFeedbackPushReceipt

interface SessionFeedbackPushReceiptPersistencePort {
    fun saveAll(receipts: Collection<SessionFeedbackPushReceipt>)

    fun findMemberIdsBySessionId(sessionId: Long): Set<MemberId>
}
