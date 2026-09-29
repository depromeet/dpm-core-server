package core.domain.member.port.outbound

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId

interface MemberOAuthPersistencePort {
    fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ): MemberOAuth

    fun relinkToMember(
        provider: OAuthProvider,
        externalId: String,
        member: Member,
    )

    fun findByProviderAndExternalId(
        provider: OAuthProvider,
        externalId: String,
    ): MemberOAuth?

    fun findMemberIdsByProvider(provider: OAuthProvider): List<MemberId>

    fun findById(id: MemberOAuthId): MemberOAuth?

    fun updateEmail(
        provider: OAuthProvider,
        externalId: String,
        email: String,
    )

    fun deleteAllByMemberId(memberId: MemberId)
}
