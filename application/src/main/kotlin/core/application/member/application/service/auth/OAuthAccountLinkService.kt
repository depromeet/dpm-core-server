package core.application.member.application.service.auth

import core.application.member.application.exception.MemberLoginMethodAlreadyLinkedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.security.oauth.apple.AppleIdTokenValidator
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.kakao.KakaoTokenExchangeService
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OAuthAccountLinkService(
    private val appleTokenExchangeService: AppleTokenExchangeService,
    private val appleIdTokenValidator: AppleIdTokenValidator,
    private val kakaoTokenExchangeService: KakaoTokenExchangeService,
    private val memberPersistencePort: MemberPersistencePort,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
) {
    @Transactional
    fun linkApple(
        memberId: MemberId,
        authorizationCode: String,
    ) {
        val tokenResponse = appleTokenExchangeService.getTokens(authorizationCode)
        val claims = appleIdTokenValidator.verify(tokenResponse.id_token)
        val externalId = claims.subject ?: throw IllegalArgumentException("Invalid ID Token: sub missing")
        val email = claims["email"] as? String ?: throw IllegalArgumentException("Invalid ID Token: email missing")

        linkOAuth(
            memberId = memberId,
            provider = OAuthProvider.APPLE,
            externalId = externalId,
            email = email,
            name = null,
        )
    }

    @Transactional
    fun linkKakao(
        memberId: MemberId,
        authorizationCode: String,
    ) {
        val kakaoUser = kakaoTokenExchangeService.getUser(authorizationCode)

        linkOAuth(
            memberId = memberId,
            provider = OAuthProvider.KAKAO,
            externalId = kakaoUser.externalId,
            email = kakaoUser.email,
            name = kakaoUser.name,
        )
    }

    private fun linkOAuth(
        memberId: MemberId,
        provider: OAuthProvider,
        externalId: String,
        email: String,
        name: String?,
    ) {
        val member = memberPersistencePort.findById(memberId) ?: throw MemberNotFoundException()

        val existingOAuth = memberOAuthPersistencePort.findByProviderAndExternalId(provider, externalId)
        if (existingOAuth != null && existingOAuth.memberId != memberId) {
            throw MemberLoginMethodAlreadyLinkedException()
        }

        ensureEmailNotOwnedByAnotherMember(memberId, email)

        val linkedProviders =
            memberOAuthPersistencePort
                .findAllByMemberId(memberId)
                .map { it.provider }
                .toSet()
        val providersAfterLink = linkedProviders + provider

        val nextEmail =
            when {
                provider == OAuthProvider.KAKAO -> email
                OAuthProvider.KAKAO in providersAfterLink -> member.email ?: email
                else -> email
            }
        val nextSignupEmail =
            when {
                provider == OAuthProvider.APPLE -> email
                provider == OAuthProvider.KAKAO && OAuthProvider.APPLE in linkedProviders -> member.signupEmail
                provider == OAuthProvider.KAKAO -> email
                else -> member.signupEmail
            }
        val nextName = resolveNextName(provider, name)

        memberPersistencePort.updateOAuthIdentity(
            memberId = memberId,
            name = nextName,
            email = nextEmail,
            signupEmail = nextSignupEmail,
        )

        if (existingOAuth == null) {
            memberOAuthPersistencePort.save(
                MemberOAuth.of(
                    externalId = externalId,
                    provider = provider,
                    memberId = memberId,
                ),
                member.copyIdentity(
                    name = nextName ?: member.name,
                    email = nextEmail,
                    signupEmail = nextSignupEmail,
                ),
            )
        }
    }

    private fun ensureEmailNotOwnedByAnotherMember(
        memberId: MemberId,
        email: String,
    ) {
        val ownedByAnotherMember =
            memberPersistencePort
                .findAllByEmailOrSignupEmail(email)
                .any { it.id != memberId }

        if (ownedByAnotherMember) {
            throw MemberLoginMethodAlreadyLinkedException()
        }
    }

    private fun resolveNextName(
        provider: OAuthProvider,
        name: String?,
    ): String? =
        if (provider == OAuthProvider.KAKAO) {
            name?.trim()?.takeIf { it.isNotBlank() }
        } else {
            null
        }

    private fun Member.copyIdentity(
        name: String,
        email: String,
        signupEmail: String,
    ): Member =
        Member(
            id = id,
            name = name,
            email = email,
            signupEmail = signupEmail,
            part = part,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
