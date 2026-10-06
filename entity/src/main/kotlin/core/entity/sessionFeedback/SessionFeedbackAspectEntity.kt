package core.entity.sessionFeedback

import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackQuestion
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "session_feedback_aspects",
    indexes = [
        Index(name = "idx_session_feedback_aspects_feedback_id", columnList = "session_feedback_id"),
    ],
)
class SessionFeedbackAspectEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_feedback_aspect_id", nullable = false, updatable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "session_feedback_id",
        nullable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val feedback: SessionFeedbackEntity,
    @Column(name = "question", nullable = false, length = 32)
    val question: String,
    @Column(name = "aspect", nullable = false, length = 32)
    val aspect: String,
) {
    companion object {
        fun of(
            feedback: SessionFeedbackEntity,
            question: SessionFeedbackQuestion,
            aspect: SessionFeedbackAspect,
        ): SessionFeedbackAspectEntity =
            SessionFeedbackAspectEntity(
                id = 0L,
                feedback = feedback,
                question = question.name,
                aspect = aspect.name,
            )
    }
}
