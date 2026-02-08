package core.domain.member.port.inbound

import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId

interface MemberQueryUseCase {
    /**
     * 멤버 식별자에 해당하는 멤버의 이름과 권한 정보를 조회함.
     *
     * @author LeeHanEum
     * @since 2025.07.27
     */
    fun getMemberNameRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel>
    // TODO: Query Model 떼야함

    fun getMembersByIds(memberIds: List<MemberId>): List<Member>

    fun getAll(): List<Member>

    fun getMemberById(memberId: MemberId): Member

    fun getMemberTeamId(memberId: MemberId): TeamId

    fun findAllMemberIdsByCohortIdAndAuthorityId(
        cohortId: CohortId,
        authorityId: Long,
    ): List<MemberId>
}
