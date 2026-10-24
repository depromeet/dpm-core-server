package core.entity.sessionFeedback

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackPushReceipt
import core.domain.sessionFeedback.vo.SessionFeedbackPushReceiptId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "session_feedback_push_receipts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_session_feedback_push_receipts_session_member",
            columnNames = ["session_id", "member_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_session_feedback_push_receipts_session_id", columnList = "session_id"),
    ],
)
class SessionFeedbackPushReceiptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_feedback_push_receipt_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "session_id", nullable = false)
    val sessionId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant,
) {
    fun toDomain(): SessionFeedbackPushReceipt =
        SessionFeedbackPushReceipt(
            id = SessionFeedbackPushReceiptId(this.id),
            sessionId = SessionId(this.sessionId),
            memberId = MemberId(this.memberId),
            sentAt = this.sentAt,
        )

    companion object {
        fun from(domainModel: SessionFeedbackPushReceipt): SessionFeedbackPushReceiptEntity =
            SessionFeedbackPushReceiptEntity(
                id = domainModel.id?.value ?: 0L,
                sessionId = domainModel.sessionId.value,
                memberId = domainModel.memberId.value,
                sentAt = domainModel.sentAt,
            )
    }
}
