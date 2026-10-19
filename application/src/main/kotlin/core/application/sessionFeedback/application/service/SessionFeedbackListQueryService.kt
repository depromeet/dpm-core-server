package core.application.sessionFeedback.application.service

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.sessionFeedback.presentation.response.SessionListFeedbackResponse
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class SessionFeedbackListQueryService(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val feedbackPersistencePort: SessionFeedbackPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val clock: Clock,
) {
    fun buildFor(
        sessions: List<Session>,
        memberId: MemberId?,
    ): Map<Long, SessionListFeedbackResponse> {
        if (sessions.isEmpty()) return emptyMap()
        val sessionIds = sessions.mapNotNull { it.id?.value }
        if (sessionIds.isEmpty()) return emptyMap()

        val formsBySessionId =
            feedbackFormPersistencePort
                .findAllBySessionIds(sessionIds)
                .associateBy { it.sessionId.value }
        val now = clock.instant()

        return sessionIds
            .mapNotNull { sessionId ->
                val form = formsBySessionId[sessionId] ?: return@mapNotNull null
                sessionId to toResponse(form, sessionId, memberId, now)
            }.toMap()
    }

    private fun toResponse(
        form: SessionFeedbackForm,
        sessionId: Long,
        memberId: MemberId?,
        now: java.time.Instant,
    ): SessionListFeedbackResponse {
        val status = form.statusAt(now)
        return SessionListFeedbackResponse(
            status = status,
            endAt = instantToLocalDateTime(form.endAt),
            canSubmit = memberId?.let { canSubmit(it, sessionId, status) } ?: false,
        )
    }

    private fun canSubmit(
        memberId: MemberId,
        sessionId: Long,
        status: SessionFeedbackStatus,
    ): Boolean {
        if (status != SessionFeedbackStatus.IN_PROGRESS) return false

        val attendance = attendancePersistencePort.findAttendanceBy(sessionId, memberId.value) ?: return false
        val isTarget = attendance.status == AttendanceStatus.PRESENT || attendance.status == AttendanceStatus.LATE
        if (!isTarget) return false

        return !feedbackPersistencePort.existsBySessionIdAndMemberId(sessionId, memberId.value)
    }
}
