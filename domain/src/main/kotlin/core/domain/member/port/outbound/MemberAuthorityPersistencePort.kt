package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberAuthority

interface MemberAuthorityPersistencePort {
    fun save(memberAuthority: MemberAuthority)

    fun findAuthorityNamesByMemberId(memberId: Long): List<String>

    fun softDeleteAllByMemberId(memberId: Long)
}
