package core.application.sessionFeedback.application.service

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.sessionFeedback.presentation.response.SessionFeedbackPendingResponse
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.member.vo.MemberId
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class SessionFeedbackPendingQueryService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val feedbackPersistencePort: SessionFeedbackPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val clock: Clock,
) {
    fun findPending(memberId: MemberId): SessionFeedbackPendingResponse? {
        val now = clock.instant()
        val activeForms = feedbackFormPersistencePort.findAllInProgressAt(now)
        if (activeForms.isEmpty()) return null

        val pickedForm =
            activeForms.firstOrNull { form ->
                isCandidate(form, memberId)
            } ?: return null

        val session = sessionPersistencePort.findSessionById(pickedForm.sessionId.value) ?: return null

        return SessionFeedbackPendingResponse(
            sessionId = session.id?.value ?: pickedForm.sessionId.value,
            week = session.week,
            sessionName = session.eventName,
            endAt = instantToLocalDateTime(pickedForm.endAt),
        )
    }

    private fun isCandidate(
        form: SessionFeedbackForm,
        memberId: MemberId,
    ): Boolean {
        val sessionId = form.sessionId.value
        val attendance = attendancePersistencePort.findAttendanceBy(sessionId, memberId.value) ?: return false
        val isTarget = attendance.status == AttendanceStatus.PRESENT || attendance.status == AttendanceStatus.LATE
        if (!isTarget) return false
        return !feedbackPersistencePort.existsBySessionIdAndMemberId(sessionId, memberId.value)
    }
}
