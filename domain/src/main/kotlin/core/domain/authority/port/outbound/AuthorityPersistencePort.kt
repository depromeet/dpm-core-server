package core.domain.authority.port.outbound

import core.domain.authority.aggregate.Authority
import core.domain.authority.vo.AuthorityId
import core.domain.member.vo.MemberId

interface AuthorityPersistencePort {
    fun findAll(): List<Authority>

    fun findAllByMemberExternalId(externalId: String): List<String>

    fun findAllByMemberId(memberId: MemberId): List<String>

    fun findAuthorityIdByName(authorityName: String): AuthorityId?
}
