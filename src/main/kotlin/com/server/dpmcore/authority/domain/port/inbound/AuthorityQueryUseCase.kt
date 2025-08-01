package com.server.dpmcore.authority.domain.port.inbound

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.member.member.domain.model.MemberId

interface AuthorityQueryUseCase {
    fun getAuthoritiesByExternalId(externalId: String): List<String>

    fun getAuthoritiesByMemberId(memberId: MemberId): List<String>

    fun getAuthorityIdByName(authorityName: AuthorityType): AuthorityId
}
