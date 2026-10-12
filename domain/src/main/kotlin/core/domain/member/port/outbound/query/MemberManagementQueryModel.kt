package core.domain.member.port.outbound.query

import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import java.time.Instant

/** 관리 대상 기수의 회원 기본 정보. 소속/팀 이력과 무관하게 회원당 한 행이다. */
data class MemberManagementQueryModel(
    val memberId: Long,
    val name: String,
    val email: String?,
    val part: MemberPart?,
    val status: MemberStatus,
    val cohortId: Long?,
    val teamNumber: Int,
    val updatedAt: Instant?,
)
