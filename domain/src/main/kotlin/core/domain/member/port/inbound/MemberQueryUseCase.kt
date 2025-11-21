package core.domain.member.port.inbound

import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.query.MemberNameAuthorityQueryModel
import core.domain.member.vo.MemberId

interface MemberQueryUseCase {
    /**
     * 멤버 식별자에 해당하는 멤버의 이름과 권한 정보를 조회함.
     *
     * @author LeeHanEum
     * @since 2025.07.27
     */
    fun getMemberNameAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel>
    // TODO: Query Model 떼야함

    fun getMembersByIds(memberIds: List<MemberId>): List<Member>
}
