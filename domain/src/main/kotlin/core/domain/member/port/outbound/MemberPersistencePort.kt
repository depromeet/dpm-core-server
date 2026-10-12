package core.domain.member.port.outbound

import core.domain.authorization.vo.RoleId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.port.outbound.query.MemberManagementQueryModel
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.port.outbound.query.MemberOverviewQueryModel
import core.domain.member.vo.MemberId

interface MemberPersistencePort {
    /** 회원의 삭제 상태와 기수 소속을 최신 상태로 읽는다. 쓰기 트랜잭션에서 ID 순서로 잠근다. */
    fun lockApprovalTargets(memberIds: List<Long>): List<MemberApprovalTarget>

    /** 현재 기수 소속 또는 어느 기수에도 속하지 않은 PENDING. 삭제/탈퇴 회원은 제외한다. */
    fun findManagementMembers(cohortId: Long): List<MemberManagementQueryModel>

    /** 현재 기수의 승인된 멤버를 ID 순서로 잠그고 수정 가능한 ID를 반환한다. 쓰기 트랜잭션 안에서 호출한다. */
    fun lockApprovedManagementMemberIds(
        memberIds: List<Long>,
        cohortId: Long,
    ): List<Long>

    /** 지정한 필드만 변경한다. 연결 정보만 변경된 멤버도 updatedAt을 갱신한다. */
    fun updateManagementFields(
        memberIds: List<Long>,
        updatePart: Boolean,
        part: MemberPart?,
        status: MemberStatus?,
        changedAssociationMemberIds: Set<Long>,
    )

    fun save(member: Member): Member

    fun findBySignupEmail(email: String): Member?

    fun findAllBySignupEmail(email: String): List<Member>

    fun findById(memberId: MemberId): Member?

    fun findAllByIds(ids: List<MemberId>): List<Member>

    fun existsById(memberId: Long): Boolean

    fun existsDeletedMemberById(memberId: Long): Boolean

    fun findByNameAndSignupEmail(
        name: String,
        signupEmail: String,
    ): Member?

    fun findAllMemberIdByRoleIds(roleIds: List<RoleId>): List<MemberId>

    fun findAllByCohort(value: String): List<MemberId>

    fun findAllByCohortId(cohortId: CohortId): List<MemberId>

    fun findAllMemberIdsByCohortIdAndAuthorityId(
        cohortId: CohortId,
        authorityId: AuthorityId,
    ): List<MemberId>

    fun findMemberNameAndRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel>

    fun findAllOrderedByHighestCohortAndStatus(
        latest: Boolean?,
        latestCohortId: Long,
    ): List<MemberOverviewQueryModel>

    fun findMemberTeamNumberByMemberId(memberId: MemberId): Int?

    fun findMemberTeamNumberByMemberIds(memberIds: List<MemberId>): Map<Long, Int>

    fun findMemberTeamIdByMemberId(memberId: MemberId): Long?

    fun findMemberTeamIdByMemberIdAndCohortId(
        memberId: MemberId,
        cohortId: CohortId,
    ): Long?

    fun findAll(): List<Member>

    fun anonymizeIdentity(
        memberId: MemberId,
        email: String,
        signupEmail: String,
    )

    fun hardDeleteById(memberId: MemberId)
}
