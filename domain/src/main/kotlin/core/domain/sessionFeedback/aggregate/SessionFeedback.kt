package core.domain.sessionFeedback.aggregate

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackQuestion
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import core.domain.sessionFeedback.vo.SessionFeedbackId
import java.time.Instant

class SessionFeedback(
    val id: SessionFeedbackId? = null,
    val sessionId: SessionId,
    val memberId: MemberId,
    val satisfaction: SessionFeedbackSatisfaction,
    val likedAspects: List<SessionFeedbackAspect>,
    val improvementAspects: List<SessionFeedbackAspect>,
    val likedEtc: String?,
    val improvementEtc: String?,
    val freeComment: String?,
    val submittedAt: Instant,
) {
    fun aspectsOf(question: SessionFeedbackQuestion): List<SessionFeedbackAspect> =
        when (question) {
            SessionFeedbackQuestion.LIKED -> likedAspects
            SessionFeedbackQuestion.IMPROVEMENT -> improvementAspects
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionFeedback) return false
        return id == other.id && sessionId == other.sessionId && memberId == other.memberId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + memberId.hashCode()
        return result
    }

    override fun toString(): String =
        "SessionFeedback(id=$id, sessionId=$sessionId, memberId=$memberId, satisfaction=$satisfaction)"

    companion object {
        fun create(
            sessionId: SessionId,
            memberId: MemberId,
            satisfaction: SessionFeedbackSatisfaction,
            likedAspects: List<SessionFeedbackAspect>,
            improvementAspects: List<SessionFeedbackAspect>,
            likedEtc: String?,
            improvementEtc: String?,
            freeComment: String?,
        ): SessionFeedback =
            SessionFeedback(
                sessionId = sessionId,
                memberId = memberId,
                satisfaction = satisfaction,
                likedAspects = likedAspects.toList(),
                improvementAspects = improvementAspects.toList(),
                likedEtc = normalizeEtc(likedAspects, likedEtc),
                improvementEtc = normalizeEtc(improvementAspects, improvementEtc),
                freeComment = freeComment?.takeIf { it.isNotBlank() },
                submittedAt = Instant.now(),
            )

        private fun normalizeEtc(
            aspects: List<SessionFeedbackAspect>,
            etc: String?,
        ): String? =
            if (SessionFeedbackAspect.ETC in aspects) {
                etc?.takeIf { it.isNotBlank() }
            } else {
                null
            }
    }
}
