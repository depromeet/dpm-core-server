package core.domain.member.port.inbound

import core.domain.authority.vo.AuthorityId
import core.domain.member.vo.MemberId

interface MemberQueryByAuthorityUseCase {
    /**
     * 권한 식별자 목록에 해당하는 모든 멤버 식별자를 조회함.
     *
     * @author leehaneum
     * @since 2025.07.22
     */
    fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId>
}
