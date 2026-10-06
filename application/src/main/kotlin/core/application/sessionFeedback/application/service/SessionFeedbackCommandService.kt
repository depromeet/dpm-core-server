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

/**
 * `POST /v2/sessions/{sessionId}/feedbacks` 제출 처리.
 *
 * 검증 순서는 스펙 §11-4 #6 과 동일하다. 진입 API(`myStatus`) 와 같은 우선순위를 유지해
 * 같은 시점에 화면/에러가 어긋나지 않도록 한다. 중복 제출은 unique(session_id, member_id) 로
 * 2중 방어하며, 동시 호출로 DataIntegrityViolationException 이 나면 409-01 로 변환한다.
 */
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
        // 3. 세션 존재
        sessionPersistencePort.findSessionById(sessionId.value) ?: throw SessionNotFoundException()

        // 4. 피드백 OFF
        val form = feedbackFormPersistencePort.findBySessionId(sessionId.value)
            ?: throw FeedbackDisabledException()

        // 5. 출석/지각 대상자
        if (!isTarget(sessionId, memberId)) throw NotFeedbackTargetException()

        // 6. 이미 제출
        if (feedbackPersistencePort.existsBySessionIdAndMemberId(sessionId.value, memberId.value)) {
            throw AlreadySubmittedFeedbackException()
        }

        // 7/8. 기간 체크
        val now = clock.instant()
        if (now.isBefore(form.startAt)) throw FeedbackNotStartedException()
        if (!now.isBefore(form.endAt)) throw FeedbackClosedException()

        // 9/10/11/12. 응답값 검증 (문항별)
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
        // 400-06: 1~2 개만 허용
        if (aspects.isEmpty() || aspects.size > 2) throw InvalidAspectCountException()
        // 400-07: 중복 선택 금지
        if (aspects.distinct().size != aspects.size) throw DuplicatedAspectException()
        // 400-08: NOTHING 과 다른 항목 동시 선택 금지
        if (SessionFeedbackAspect.NOTHING in aspects && aspects.size > 1) throw ExclusiveAspectCombinedException()
        // 400-09: ETC 선택 시 텍스트 필수
        if (SessionFeedbackAspect.ETC in aspects && etc.isNullOrBlank()) throw EtcTextRequiredException()
    }
}
