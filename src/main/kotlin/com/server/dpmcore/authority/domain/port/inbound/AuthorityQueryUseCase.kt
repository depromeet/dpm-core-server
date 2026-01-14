package com.server.dpmcore.authority.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId

interface AuthorityQueryUseCase {
    fun getAuthoritiesByExternalId(externalId: String): List<String>

    fun getAuthoritiesByMemberId(memberId: MemberId): List<String>
}
