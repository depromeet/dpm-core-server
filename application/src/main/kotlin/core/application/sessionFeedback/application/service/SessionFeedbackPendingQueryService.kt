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

/**
 * 홈 카드용: 지금 작성 가능한(`canSubmit=true`) 세션 1건을 반환한다.
 *
 * 수집 중(`IN_PROGRESS`) 설문을 `endAt` 오름차순으로 돌면서 멤버가 대상이고 미제출인 첫 세션을
 * 찾는다. 활성 설문은 72h 윈도우라 동시에 떠 있는 수가 작다는 가정 하에 N+1 질의를 허용한다.
 */
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
