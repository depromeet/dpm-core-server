package core.application.sessionFeedback.application.service

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.session.application.exception.SessionNotFoundException
import core.application.sessionFeedback.presentation.response.SessionFeedbackMyResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackQuestionsResponse
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.member.vo.MemberId
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
@Transactional(readOnly = true)
class SessionFeedbackMyQueryService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val feedbackPersistencePort: SessionFeedbackPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val clock: Clock,
) {
    fun getMyFeedback(
        sessionId: SessionId,
        memberId: MemberId,
    ): SessionFeedbackMyResponse {
        val session = sessionPersistencePort.findSessionById(sessionId.value) ?: throw SessionNotFoundException()
        val form = feedbackFormPersistencePort.findBySessionId(sessionId.value)
        val now = clock.instant()

        val myStatus = resolveStatus(form, sessionId, memberId, now)

        return SessionFeedbackMyResponse(
            sessionId = session.id?.value ?: sessionId.value,
            week = session.week,
            sessionName = session.eventName,
            startAt = form?.startAt?.let(::instantToLocalDateTime),
            endAt = form?.endAt?.let(::instantToLocalDateTime),
            myStatus = myStatus,
            questions = if (myStatus == SessionFeedbackMyStatus.AVAILABLE) SessionFeedbackQuestionsResponse.DEFAULT else null,
        )
    }

    private fun resolveStatus(
        form: SessionFeedbackForm?,
        sessionId: SessionId,
        memberId: MemberId,
        now: Instant,
    ): SessionFeedbackMyStatus {
        if (form == null) return SessionFeedbackMyStatus.DISABLED
        if (!isTarget(sessionId, memberId)) return SessionFeedbackMyStatus.NOT_TARGET
        if (feedbackPersistencePort.existsBySessionIdAndMemberId(sessionId.value, memberId.value)) {
            return SessionFeedbackMyStatus.SUBMITTED
        }
        if (now.isBefore(form.startAt)) return SessionFeedbackMyStatus.BEFORE_START
        if (!now.isBefore(form.endAt)) return SessionFeedbackMyStatus.CLOSED
        return SessionFeedbackMyStatus.AVAILABLE
    }

    private fun isTarget(
        sessionId: SessionId,
        memberId: MemberId,
    ): Boolean {
        val attendance =
            attendancePersistencePort.findAttendanceBy(sessionId.value, memberId.value) ?: return false
        return attendance.status == AttendanceStatus.PRESENT || attendance.status == AttendanceStatus.LATE
    }
}
