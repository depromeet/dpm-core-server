package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberRole

interface MemberRolePersistencePort {
    fun save(memberRole: MemberRole)

    fun findAuthorityNamesByMemberId(memberId: Long): List<String>

    fun softDeleteAllByMemberId(memberId: Long)
}
