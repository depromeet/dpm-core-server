package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.MemberId

interface MemberQueryByAuthorityUseCase {
    /**
     * 권한 ID 목록에 해당하는 모든 멤버 ID를 조회함.
     *
     * @author leehaneum
     * @since 2025.07.22
     */
    fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId>
}
