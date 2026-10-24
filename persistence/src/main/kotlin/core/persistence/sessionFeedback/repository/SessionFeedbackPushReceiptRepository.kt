package core.persistence.sessionFeedback.repository

import core.domain.member.vo.MemberId
import core.domain.sessionFeedback.aggregate.SessionFeedbackPushReceipt
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPushReceiptPersistencePort
import core.entity.sessionFeedback.SessionFeedbackPushReceiptEntity
import org.springframework.stereotype.Repository

@Repository
class SessionFeedbackPushReceiptRepository(
    private val jpaRepository: SessionFeedbackPushReceiptJpaRepository,
) : SessionFeedbackPushReceiptPersistencePort {
    override fun saveAll(receipts: Collection<SessionFeedbackPushReceipt>) {
        if (receipts.isEmpty()) return
        jpaRepository.saveAll(receipts.map { SessionFeedbackPushReceiptEntity.from(it) })
    }

    override fun findMemberIdsBySessionId(sessionId: Long): Set<MemberId> =
        jpaRepository.findMemberIdsBySessionId(sessionId).mapTo(mutableSetOf()) { MemberId(it) }
}
