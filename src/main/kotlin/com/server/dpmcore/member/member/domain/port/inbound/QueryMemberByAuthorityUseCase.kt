package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.MemberId

interface QueryMemberByAuthorityUseCase {
    fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId>
}
