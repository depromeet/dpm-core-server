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
import java.time.Instant

interface MemberPersistencePort {
    /** 이미 잠근 신규 승인 회원의 승인 완료 화면을 미확인으로 초기화한다. */
    fun resetApprovalWelcome(memberIds: List<Long>)

    /** 잠근 승인 회원의 확인 상태를 기록한다. 반복 호출은 확인 상태를 유지한다. */
    fun markApprovalWelcomeSeen(memberId: Long)

    /** 삭제 트랜잭션에서 이미 잠그고 검증한 회원의 상태와 삭제 시각만 변경한다. */
    fun softDeleteMembers(memberIds: List<Long>)

    /** 회원 행을 잠근 뒤 대상 중 공유 기록 소유자 ID를 중복 없이 오름차순으로 조회한다. */
    fun findSharedRecordOwnerIds(memberIds: List<Long>): List<Long>

    /** 토큰 인증 및 재발급에서 현재 삭제/탈퇴 여부를 확인한다. */
    fun isLoginAvailable(memberId: Long): Boolean

    /** 회원의 삭제 상태와 기수 소속을 최신 상태로 읽는다. 쓰기 트랜잭션에서 ID 순서로 잠근다. */
    fun lockApprovalTargets(memberIds: List<Long>): List<MemberApprovalTarget>

    /** 현재 기수 소속 또는 어느 기수에도 속하지 않은 PENDING. 삭제/탈퇴 회원은 제외한다. */
    fun findManagementMembers(cohortId: Long): List<MemberManagementQueryModel>

    /** 미승인 회원의 최신 재신청 시각, 재신청 이력이 없으면 최초 생성 시각을 일괄 조회한다. */
    fun findPendingRequestTimes(memberIds: List<Long>): Map<Long, Instant>

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

    /**
     * 회원과 연관 업무 데이터를 영구 삭제한다.
     * 배지 대상 기록은 호출자가 MemberPurgedEvent를 발행하여 같은 트랜잭션의 마지막 단계에서 정리한다.
     *
     * @return 함께 삭제된 이미지의 버킷 객체 키. 버킷 정리는 호출자가 커밋 후에 한다.
     */
    fun hardDeleteById(memberId: MemberId): List<String>
}
