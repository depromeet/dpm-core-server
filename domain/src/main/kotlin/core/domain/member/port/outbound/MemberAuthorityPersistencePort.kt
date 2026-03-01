package core.domain.member.port.outbound

import core.domain.member.vo.MemberId

interface MemberAuthorityPersistencePort {
    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityName: String,
    )

    fun revokeAuthority(
        memberId: MemberId,
        authorityName: String,
    )
}
