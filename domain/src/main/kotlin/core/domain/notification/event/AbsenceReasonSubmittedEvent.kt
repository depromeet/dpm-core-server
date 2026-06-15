package core.domain.notification.event

import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

/**
 * 디퍼가 결석 사유서를 제출했을 때 발행되는 이벤트.
 *
 * 해당 기수 운영진에게 푸시 알림을 발송하기 위한 정보를 담는다.
 */
data class AbsenceReasonSubmittedEvent(
    val cohortId: CohortId,
    val sessionId: SessionId,
    val memberId: MemberId,
    val submitterName: String,
    val week: Int,
)
