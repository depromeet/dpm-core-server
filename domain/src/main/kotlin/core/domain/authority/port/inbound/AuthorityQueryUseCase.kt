package core.domain.authority.port.inbound

import core.domain.authority.enums.AuthorityType
import core.domain.authority.vo.AuthorityId
import core.domain.member.vo.MemberId

interface AuthorityQueryUseCase {
    fun getAuthoritiesByExternalId(externalId: String): List<String>

    fun getAuthoritiesByMemberId(memberId: MemberId): List<String>

    fun getAuthorityIdByType(authorityType: AuthorityType): AuthorityId
}
