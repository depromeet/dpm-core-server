package core.domain.member.port.outbound

import core.domain.cohort.vo.AuthorityId
import core.domain.member.vo.MemberId

interface MemberAuthorityPersistencePort {
    fun findAuthorityNamesByMemberId(memberId: MemberId): List<String>

    fun findActiveAuthorityIdsByMemberId(memberId: MemberId): List<Long>

    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityName: String,
    )

    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityId: AuthorityId,
    )

    fun revokeAuthority(
        memberId: MemberId,
        authorityName: String,
    )

    fun revokeAuthority(
        memberId: MemberId,
        authorityId: AuthorityId,
    )

    fun revokeAllByMemberId(memberId: MemberId)
}
