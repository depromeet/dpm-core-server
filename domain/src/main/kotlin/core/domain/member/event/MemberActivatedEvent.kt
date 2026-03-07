package core.domain.member.event

import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId

data class MemberActivatedEvent(
    val memberId: MemberId,
    val cohortId: CohortId,
)
