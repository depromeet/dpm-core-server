package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.query.MemberNameAuthorityQueryModel

interface MemberQueryUseCase {
    fun getMemberNameAuthorityByMemberId(memberId: MemberId): MemberNameAuthorityQueryModel
}
