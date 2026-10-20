package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberExceptionCode
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.service.OAuthAuthorizationUrl
import core.application.security.oauth.service.OAuthAuthorizationUrlService
import core.application.security.oauth.token.LoginIdentityValidator
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberAccountPersistencePort
import core.domain.member.port.outbound.OAuthLinkRequest
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class MemberAccountLinkRequestService(
    private val accounts: MemberAccountPersistencePort,
    private val locks: MemberIdentityLockService,
    private val identities: LoginIdentityValidator,
    private val redirects: OAuthRedirectUriValidator,
    private val urls: OAuthAuthorizationUrlService,
) {
    @Transactional
    fun start(
        memberId: Long,
        identity: LoginIdentity?,
        provider: OAuthProvider,
        redirectUri: String,
    ): OAuthAuthorizationUrl {
        locks.lockMember(MemberId(memberId))
        if (!identities.isValid(
                memberId,
                identity,
            )
        ) {
            throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_REAUTH_REQUIRED)
        }
        val redirect = redirects.validate(redirectUri)
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val hash = TokenHasher.sha256Hex(state)
        val verifier =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                },
            )
        val challenge =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            )
        accounts.saveLinkRequest(
            OAuthLinkRequest(hash, memberId, provider, redirect, verifier, Instant.now().plusSeconds(300)),
        )
        return urls.buildLinkAuthorizationUrl(provider.name, redirect, state, hash, challenge)
    }

    // A failed provider exchange still consumes the request. Retry must start a fresh authorization.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun consume(
        memberId: Long,
        provider: OAuthProvider,
        state: String,
    ): OAuthLinkRequest {
        val hash = TokenHasher.sha256Hex(state)
        val request =
            accounts.lockLinkRequest(hash)
                ?: throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID)
        if (request.memberId != memberId || request.provider != provider || !request.expiresAt.isAfter(Instant.now())) {
            throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID)
        }
        accounts.deleteLinkRequest(hash)
        return request
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    fun removeExpired() {
        accounts.deleteExpiredLinkRequests(Instant.now())
    }
}
