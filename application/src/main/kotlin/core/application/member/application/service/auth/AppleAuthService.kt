package core.application.member.application.service.auth

import core.application.member.application.exception.MemberNotFoundException
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppleAuthService(
    private val appleTokenExchangeService: AppleTokenExchangeService,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val appleIdTokenValidator: core.application.security.oauth.apple.AppleIdTokenValidator,
) {
    @Transactional
    fun login(authorizationCode: String): AuthTokenResponse {
        // 1. Exchange Code for Tokens
        val tokenResponse = appleTokenExchangeService.getTokens(authorizationCode)

        // 2. Verify ID Token
        val claims = appleIdTokenValidator.verify(tokenResponse.id_token)
        val externalId = claims.subject ?: throw IllegalArgumentException("Invalid ID Token: sub missing")

        // 3. Find check existing Member
        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(OAuthProvider.APPLE, externalId)
                ?: throw MemberNotFoundException()

        val member =
            memberPersistencePort.findById(memberOAuth.memberId.value)
                ?: throw IllegalStateException("MemberOAuth exists but Member not found")

        // 4. Issue App Tokens
        val accessToken = jwtTokenProvider.generateAccessToken(member.id!!.toString())
        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id!!.toString())

        // 5. Save Refresh Token
        val refreshTokenEntity =
            refreshTokenPersistencePort.findByMemberId(member.id!!.value)
                ?.apply { rotate(refreshToken) }
                ?: RefreshToken.create(member.id!!, refreshToken)
        refreshTokenPersistencePort.save(refreshTokenEntity)

        return AuthTokenResponse(accessToken, refreshToken)
    }
}

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
