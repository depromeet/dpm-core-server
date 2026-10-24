package core.domain.member.port.outbound

import core.domain.member.enums.MemberActivityStatus
import java.time.Instant

data class MemberCohortActivity(
    val memberId: Long,
    val cohortId: Long,
    val status: MemberActivityStatus,
    val suspendedAt: Instant? = null,
)

interface MemberActivityPersistencePort {
    /** 기수 전환과 회원 승인의 순서를 일치시킨다. 회원 잠금보다 먼저 호출한다. */
    fun lockCohorts()

    fun find(memberId: Long, cohortId: Long): MemberCohortActivity?

    fun findAll(memberIds: List<Long>, cohortId: Long): Map<Long, MemberCohortActivity>

    fun findByCohort(cohortId: Long): List<MemberCohortActivity>

    /** 호출자가 기수와 회원을 잠근 트랜잭션에서 상태와 이력을 함께 저장한다. */
    fun transition(memberId: Long, cohortId: Long, status: MemberActivityStatus, actorId: Long?)
}
