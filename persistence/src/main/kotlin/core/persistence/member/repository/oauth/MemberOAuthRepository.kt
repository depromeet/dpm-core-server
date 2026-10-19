package core.persistence.member.repository.oauth

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.entity.member.MemberOAuthEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.springframework.stereotype.Repository

@Repository
class MemberOAuthRepository(
    private val memberOAuthJpaRepository: MemberOAuthJpaRepository,
    private val dsl: DSLContext,
) : MemberOAuthPersistencePort {
    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ): MemberOAuth = memberOAuthJpaRepository.save(MemberOAuthEntity.of(memberOAuth, member)).toDomain()

    override fun findMemberIdsByProvider(provider: OAuthProvider): List<MemberId> =
        memberOAuthJpaRepository
            .findAllByProvider(provider.name)
            .map { MemberId(it.member.id) }

    override fun findById(id: MemberOAuthId): MemberOAuth? =
        memberOAuthJpaRepository
            .findById(id.value)
            .orElse(null)
            ?.toDomain()

    override fun relinkToMember(
        provider: OAuthProvider,
        externalId: String,
        member: Member,
    ) {
        dsl.update(MEMBER_OAUTH)
            .set(MEMBER_OAUTH.MEMBER_ID, requireNotNull(member.id).value)
            .where(MEMBER_OAUTH.PROVIDER.eq(provider.name), MEMBER_OAUTH.EXTERNAL_ID.eq(externalId))
            .execute()
    }

    override fun findByProviderAndExternalId(
        provider: OAuthProvider,
        externalId: String,
    ): MemberOAuth? =
        dsl.selectFrom(MEMBER_OAUTH)
            .where(MEMBER_OAUTH.PROVIDER.eq(provider.name), MEMBER_OAUTH.EXTERNAL_ID.eq(externalId))
            .fetchOne { row ->
                MemberOAuth(
                    id = MemberOAuthId(requireNotNull(row.memberOauthId)),
                    externalId = requireNotNull(row.externalId),
                    provider = OAuthProvider.valueOf(requireNotNull(row.provider)),
                    memberId = MemberId(requireNotNull(row.memberId)),
                    email = row.email,
                )
            }

    override fun updateEmail(
        provider: OAuthProvider,
        externalId: String,
        email: String,
    ) {
        if (email.isBlank()) return
        memberOAuthJpaRepository.updateEmail(provider.name, externalId, email)
    }

    override fun deleteAllByMemberId(memberId: MemberId) {
        memberOAuthJpaRepository.deleteAllByMemberId(memberId.value)
    }
}
