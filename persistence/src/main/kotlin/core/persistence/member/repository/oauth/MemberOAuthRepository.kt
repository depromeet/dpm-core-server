package core.persistence.member.repository.oauth

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.entity.member.MemberOAuthEntity
import org.springframework.stereotype.Repository

@Repository
class MemberOAuthRepository(
    private val memberOAuthJpaRepository: MemberOAuthJpaRepository,
) : MemberOAuthPersistencePort {
    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ) {
        memberOAuthJpaRepository.save(MemberOAuthEntity.of(memberOAuth, member))
    }

    override fun relinkToMember(
        provider: OAuthProvider,
        externalId: String,
        member: Member,
    ) {
        val existing =
            memberOAuthJpaRepository.findByProviderAndExternalId(provider.name, externalId)
                ?: return

        memberOAuthJpaRepository.save(
            MemberOAuthEntity(
                id = existing.id,
                externalId = existing.externalId,
                provider = existing.provider,
                member = core.entity.member.MemberEntity.from(member),
            ),
        )
    }

    override fun findByProviderAndExternalId(
        provider: OAuthProvider,
        externalId: String,
    ): MemberOAuth? {
        return memberOAuthJpaRepository.findByProviderAndExternalId(provider.name, externalId)?.toDomain()
    }
}
