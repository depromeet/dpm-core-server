package core.domain.absencereason.port.inbound.command

import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

/** @property imageIds null 이면 기존 첨부 유지(새 사유서는 첨부 없음), 빈 목록이면 모두 해제, 값이 있으면 그 순서로 교체 */
data class AbsenceReportCreateCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val contents: String,
    val imageIds: List<ImageId>? = null,
)
