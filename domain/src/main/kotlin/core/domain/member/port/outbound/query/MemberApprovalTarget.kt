package core.domain.member.port.outbound.query

import core.domain.member.enums.MemberStatus
import java.time.Instant

data class MemberApprovalTarget(
    val memberId: Long,
    val status: MemberStatus,
    val cohortIds: Set<Long>,
    val isDeleted: Boolean = false,
    val profileCompletedAt: Instant? = null,
)
