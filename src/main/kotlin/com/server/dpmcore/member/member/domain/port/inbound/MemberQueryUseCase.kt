package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.query.MemberNameAuthorityQueryModel

interface MemberQueryUseCase {
    /**
     * 멤버 식별자에 해당하는 멤버의 이름과 권한 정보를 조회함.
     *
     * @author LeeHanEum
     * @since 2025.07.27
     */
    fun getMemberNameAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel>
}
