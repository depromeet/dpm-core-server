package core.entity.sessionFeedback

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackQuestion
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import core.domain.sessionFeedback.vo.SessionFeedbackId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "session_feedbacks",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_session_feedbacks_session_member",
            columnNames = ["session_id", "member_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_session_feedbacks_session_id", columnList = "session_id"),
    ],
)
class SessionFeedbackEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_feedback_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "session_id", nullable = false)
    val sessionId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "satisfaction", nullable = false, length = 32)
    val satisfaction: String,
    @Column(name = "liked_etc", nullable = true, length = 500)
    val likedEtc: String?,
    @Column(name = "improvement_etc", nullable = true, length = 500)
    val improvementEtc: String?,
    @Column(name = "free_comment", nullable = true, columnDefinition = "TEXT")
    val freeComment: String?,
    @Column(name = "submitted_at", nullable = false)
    val submittedAt: Instant,
    @OneToMany(
        mappedBy = "feedback",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val aspects: MutableList<SessionFeedbackAspectEntity> = mutableListOf(),
) {
    fun toDomain(): SessionFeedback {
        val liked = aspectsFor(SessionFeedbackQuestion.LIKED)
        val improvement = aspectsFor(SessionFeedbackQuestion.IMPROVEMENT)
        return SessionFeedback(
            id = SessionFeedbackId(this.id),
            sessionId = SessionId(this.sessionId),
            memberId = MemberId(this.memberId),
            satisfaction = SessionFeedbackSatisfaction.valueOf(this.satisfaction),
            likedAspects = liked,
            improvementAspects = improvement,
            likedEtc = this.likedEtc,
            improvementEtc = this.improvementEtc,
            freeComment = this.freeComment,
            submittedAt = this.submittedAt,
        )
    }

    private fun aspectsFor(question: SessionFeedbackQuestion): List<SessionFeedbackAspect> =
        aspects
            .filter { it.question == question.name }
            .map { SessionFeedbackAspect.valueOf(it.aspect) }

    companion object {
        fun from(domainModel: SessionFeedback): SessionFeedbackEntity {
            val entity =
                SessionFeedbackEntity(
                    id = domainModel.id?.value ?: 0L,
                    sessionId = domainModel.sessionId.value,
                    memberId = domainModel.memberId.value,
                    satisfaction = domainModel.satisfaction.name,
                    likedEtc = domainModel.likedEtc,
                    improvementEtc = domainModel.improvementEtc,
                    freeComment = domainModel.freeComment,
                    submittedAt = domainModel.submittedAt,
                )
            val aspectEntities =
                buildList {
                    addAll(domainModel.likedAspects.map { SessionFeedbackAspectEntity.of(entity, SessionFeedbackQuestion.LIKED, it) })
                    addAll(
                        domainModel.improvementAspects.map {
                            SessionFeedbackAspectEntity.of(entity, SessionFeedbackQuestion.IMPROVEMENT, it)
                        },
                    )
                }
            entity.aspects.addAll(aspectEntities)
            return entity
        }
    }
}
