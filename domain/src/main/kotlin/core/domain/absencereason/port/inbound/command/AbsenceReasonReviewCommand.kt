package core.domain.absencereason.port.inbound.command

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

/**
 * 운영진의 결석 사유서 검토 요청.
 *
 * @property approved 승인 여부 (true: 승인, false: 반려)
 */
data class AbsenceReasonReviewCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val approved: Boolean,
)
