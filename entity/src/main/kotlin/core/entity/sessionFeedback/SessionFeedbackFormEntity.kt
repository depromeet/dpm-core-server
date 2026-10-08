package core.entity.sessionFeedback

import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.vo.SessionFeedbackFormId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "session_feedback_forms",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_session_feedback_forms_session_id",
            columnNames = ["session_id"],
        ),
    ],
)
class SessionFeedbackFormEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_feedback_form_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "session_id", nullable = false)
    val sessionId: Long,
    @Column(name = "start_at", nullable = false)
    val startAt: Instant,
    @Column(name = "end_at", nullable = false)
    val endAt: Instant,
    @Column(name = "push_enabled", nullable = false)
    val pushEnabled: Boolean,
    @Column(name = "push_sent_at", nullable = true)
    val pushSentAt: Instant? = null,
    @Column(name = "created_at", nullable = true)
    val createdAt: Instant? = null,
    @Column(name = "updated_at", nullable = true)
    val updatedAt: Instant? = null,
    @Column(name = "deleted_at", nullable = true)
    val deletedAt: Instant? = null,
) {
    fun toDomain(): SessionFeedbackForm =
        SessionFeedbackForm(
            id = SessionFeedbackFormId(this.id),
            sessionId = SessionId(this.sessionId),
            startAt = this.startAt,
            endAt = this.endAt,
            pushEnabled = this.pushEnabled,
            pushSentAt = this.pushSentAt,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt,
        )

    companion object {
        fun from(domainModel: SessionFeedbackForm): SessionFeedbackFormEntity =
            SessionFeedbackFormEntity(
                id = domainModel.id?.value ?: 0L,
                sessionId = domainModel.sessionId.value,
                startAt = domainModel.startAt,
                endAt = domainModel.endAt,
                pushEnabled = domainModel.pushEnabled,
                pushSentAt = domainModel.pushSentAt,
                createdAt = domainModel.createdAt,
                updatedAt = domainModel.updatedAt,
                deletedAt = domainModel.deletedAt,
            )
    }
}
