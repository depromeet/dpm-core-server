package core.application.member.application.service.oauth

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service

@Service
class MemberOAuthService(
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
) {
    fun findByProviderAndExternalId(
        provider: core.domain.member.enums.OAuthProvider,
        externalId: String,
    ): MemberOAuth? = memberOAuthPersistencePort.findByProviderAndExternalId(provider, externalId)
    /**
     * 멤버 가입 시, OAuth 클라이언트로부터 전달받은 제공자 정보를 저장함.
     *
     * @author LeeHanEum
     * @since 2025.07.24
     */
    fun addMemberOAuthProvider(
        member: Member,
        authAttribute: OAuthAttributes,
    ) {
        memberOAuthPersistencePort.save(
            MemberOAuth.of(
                authAttribute.getExternalId(),
                authAttribute.getProvider(),
                member.id!!,
            ),
            member,
        )
    }
}
