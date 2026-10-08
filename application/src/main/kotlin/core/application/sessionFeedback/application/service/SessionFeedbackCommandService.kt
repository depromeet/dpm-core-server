package core.application.sessionFeedback.application.service

import core.application.session.application.exception.SessionNotFoundException
import core.application.sessionFeedback.application.exception.AlreadySubmittedFeedbackException
import core.application.sessionFeedback.application.exception.DuplicatedAspectException
import core.application.sessionFeedback.application.exception.EtcTextRequiredException
import core.application.sessionFeedback.application.exception.ExclusiveAspectCombinedException
import core.application.sessionFeedback.application.exception.FeedbackClosedException
import core.application.sessionFeedback.application.exception.FeedbackDisabledException
import core.application.sessionFeedback.application.exception.FeedbackNotStartedException
import core.application.sessionFeedback.application.exception.InvalidAspectCountException
import core.application.sessionFeedback.application.exception.NotFeedbackTargetException
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.member.vo.MemberId
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class SessionFeedbackCommandService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val feedbackPersistencePort: SessionFeedbackPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val clock: Clock,
) {
    fun submit(
        sessionId: SessionId,
        memberId: MemberId,
        satisfaction: SessionFeedbackSatisfaction,
        likedAspects: List<SessionFeedbackAspect>,
        improvementAspects: List<SessionFeedbackAspect>,
        likedEtc: String?,
        improvementEtc: String?,
        freeComment: String?,
    ) {
        sessionPersistencePort.findSessionById(sessionId.value) ?: throw SessionNotFoundException()

        val form = feedbackFormPersistencePort.findBySessionId(sessionId.value)
            ?: throw FeedbackDisabledException()

        if (!isTarget(sessionId, memberId)) throw NotFeedbackTargetException()

        if (feedbackPersistencePort.existsBySessionIdAndMemberId(sessionId.value, memberId.value)) {
            throw AlreadySubmittedFeedbackException()
        }

        val now = clock.instant()
        if (now.isBefore(form.startAt)) throw FeedbackNotStartedException()
        if (!now.isBefore(form.endAt)) throw FeedbackClosedException()

        validateAspects(likedAspects, likedEtc)
        validateAspects(improvementAspects, improvementEtc)

        try {
            feedbackPersistencePort.save(
                SessionFeedback.create(
                    sessionId = sessionId,
                    memberId = memberId,
                    satisfaction = satisfaction,
                    likedAspects = likedAspects,
                    improvementAspects = improvementAspects,
                    likedEtc = likedEtc,
                    improvementEtc = improvementEtc,
                    freeComment = freeComment,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            throw AlreadySubmittedFeedbackException()
        }
    }

    private fun isTarget(
        sessionId: SessionId,
        memberId: MemberId,
    ): Boolean {
        val attendance = attendancePersistencePort.findAttendanceBy(sessionId.value, memberId.value) ?: return false
        return attendance.status == AttendanceStatus.PRESENT || attendance.status == AttendanceStatus.LATE
    }

    private fun validateAspects(
        aspects: List<SessionFeedbackAspect>,
        etc: String?,
    ) {
        if (aspects.isEmpty() || aspects.size > 2) throw InvalidAspectCountException()
        if (aspects.distinct().size != aspects.size) throw DuplicatedAspectException()
        if (SessionFeedbackAspect.NOTHING in aspects && aspects.size > 1) throw ExclusiveAspectCombinedException()
        if (SessionFeedbackAspect.ETC in aspects && etc.isNullOrBlank()) throw EtcTextRequiredException()
    }
}
