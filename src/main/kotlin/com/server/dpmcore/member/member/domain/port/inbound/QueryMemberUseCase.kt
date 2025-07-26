package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.query.MemberNameAuthorityQueryModel

interface QueryMemberUseCase {
    fun getMemberNameAuthorityByMemberId(memberId: MemberId): MemberNameAuthorityQueryModel
}
