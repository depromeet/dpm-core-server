package core.application.support

import core.domain.member.vo.MemberId
import core.domain.sessionFeedback.aggregate.SessionFeedbackPushReceipt
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPushReceiptPersistencePort
import core.domain.sessionFeedback.vo.SessionFeedbackPushReceiptId
import java.util.concurrent.atomic.AtomicLong

class FakeSessionFeedbackPushReceiptPersistencePort : SessionFeedbackPushReceiptPersistencePort {
    private val sequence = AtomicLong(0)
    private val rows = linkedMapOf<Pair<Long, Long>, SessionFeedbackPushReceipt>()

    @Synchronized
    override fun saveAll(receipts: Collection<SessionFeedbackPushReceipt>) {
        receipts.forEach { receipt ->
            val key = receipt.sessionId.value to receipt.memberId.value
            if (rows.containsKey(key)) return@forEach
            rows[key] =
                SessionFeedbackPushReceipt(
                    id = SessionFeedbackPushReceiptId(sequence.incrementAndGet()),
                    sessionId = receipt.sessionId,
                    memberId = receipt.memberId,
                    sentAt = receipt.sentAt,
                )
        }
    }

    @Synchronized
    override fun findMemberIdsBySessionId(sessionId: Long): Set<MemberId> =
        rows.values.filter { it.sessionId.value == sessionId }.mapTo(mutableSetOf()) { it.memberId }

    @Synchronized
    fun all(): List<SessionFeedbackPushReceipt> = rows.values.toList()
}
