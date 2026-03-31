package core.domain.member.port.outbound

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.MemberId

interface MemberOAuthPersistencePort {
    fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    )

    fun relinkToMember(
        provider: OAuthProvider,
        externalId: String,
        member: Member,
    )

    fun findByProviderAndExternalId(
        provider: OAuthProvider,
        externalId: String,
    ): MemberOAuth?

    fun deleteAllByMemberId(memberId: MemberId)
}
