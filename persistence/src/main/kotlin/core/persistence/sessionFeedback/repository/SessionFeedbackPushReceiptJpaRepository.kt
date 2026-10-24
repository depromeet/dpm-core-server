package core.persistence.sessionFeedback.repository

import core.entity.sessionFeedback.SessionFeedbackPushReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SessionFeedbackPushReceiptJpaRepository : JpaRepository<SessionFeedbackPushReceiptEntity, Long> {
    @Query(
        "select r.memberId from SessionFeedbackPushReceiptEntity r where r.sessionId = :sessionId",
    )
    fun findMemberIdsBySessionId(sessionId: Long): List<Long>
}
