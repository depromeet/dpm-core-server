package core.application.member.application.service.auth

import core.application.common.constant.Profile
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.core.env.Environment
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
    private val environment: Environment,
) {
    @Transactional
    fun login(
        authorizationCode: String,
        fullName: String? = null,
        familyName: String? = null,
        givenName: String? = null,
    ): AuthTokenResponse {
        val tokenResponse = appleTokenExchangeService.getTokens(authorizationCode)

        val claims = appleIdTokenValidator.verify(tokenResponse.id_token)
        val externalId = claims.subject ?: throw IllegalArgumentException("Invalid ID Token: sub missing")

        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(OAuthProvider.APPLE, externalId)

        val member =
            if (memberOAuth == null) {
                // Initial signup - create new member
                val email =
                    claims["email"] as? String
                        ?: throw IllegalArgumentException("Invalid ID Token: email missing")

                val claimName = claims["name"] as? String
                val name =
                    resolveMemberName(
                        fullName = fullName ?: claimName,
                        familyName = familyName,
                        givenName = givenName,
                    ) ?: throw IllegalArgumentException("Apple name missing for initial signup")

                val newMember =
                    memberPersistencePort.save(
                        Member.create(
                            email = email,
                            name = name,
                            activeProfile = Profile.get(environment).value,
                        ),
                    )

                // Create MemberOAuth
                memberOAuthPersistencePort.save(
                    MemberOAuth.of(
                        externalId = externalId,
                        provider = OAuthProvider.APPLE,
                        memberId = newMember.id!!,
                    ),
                    newMember,
                )

                newMember
            } else {
                // Existing member
                memberPersistencePort.findById(memberOAuth.memberId)
                    ?: throw IllegalStateException("MemberOAuth exists but Member not found")
            }

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

    private fun resolveMemberName(
        fullName: String?,
        familyName: String?,
        givenName: String?,
    ): String? {
        val normalizedFamilyName = familyName?.trim().orEmpty()
        val normalizedGivenName = givenName?.trim().orEmpty()

        if (normalizedFamilyName.isNotBlank() || normalizedGivenName.isNotBlank()) {
            return (normalizedFamilyName + normalizedGivenName).ifBlank { null }
        }

        return fullName?.trim()?.takeIf { it.isNotBlank() }
    }
}

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
