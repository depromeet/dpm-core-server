package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.presentation.response.KakaoNativeLoginResponse
import core.application.security.oauth.exception.OAuthAuthenticationFailedException
import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class KakaoNativeLoginService(
    private val kakaoUserInfoClient: KakaoUserInfoClient,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val handleMemberLoginUseCase: HandleMemberLoginUseCase,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val logger = KotlinLogging.logger { }

    fun login(kakaoAccessToken: String): KakaoNativeLoginResponse {
        val externalId = kakaoUserInfoClient.getServiceUserIdByAccessToken(kakaoAccessToken).toString()
        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(
                provider = OAuthProvider.KAKAO,
                externalId = externalId,
            )

        if (memberOAuth == null) {
            val attributes = resolveAttributes(kakaoAccessToken)
            return KakaoNativeLoginResponse.signupRequired(
                provider = attributes.getProvider().name,
                externalId = attributes.getExternalId(),
                email = attributes.getEmail(),
                name = attributes.getName(),
            )
        }

        val linkedMember = memberPersistencePort.findById(memberOAuth.memberId)

        linkedMember?.let { member ->
            if (member.deletedAt != null || member.status == MemberStatus.WITHDRAWN) {
                throw MemberDeletedException()
            }
        }

        val loginAttributes =
            if (linkedMember == null) {
                resolveAttributes(kakaoAccessToken)
            } else {
                MinimalKakaoAuthAttributes(externalId)
            }

        val loginResult =
            handleMemberLoginUseCase.handleLoginSuccess(
                loginAttributes,
            )
        val refreshToken =
            loginResult.refreshToken
                ?: throw IllegalStateException("Refresh token was not issued for Kakao native login")
        val memberId = refreshToken.memberId.value
        val accessToken = jwtTokenProvider.generateAccessToken(memberId.toString())
        val memberStatus = memberPersistencePort.findById(refreshToken.memberId)?.status

        logger.info { "Kakao native login succeeded for memberId=$memberId" }

        return KakaoNativeLoginResponse.loginSuccess(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            memberId = memberId,
            memberStatus = memberStatus,
        )
    }

    private fun resolveAttributes(kakaoAccessToken: String): OAuthAttributes =
        try {
            OAuthAttributes.of(
                KAKAO_PROVIDER_ID,
                kakaoUserInfoClient.getUserAttributesByAccessToken(kakaoAccessToken),
            ) ?: throw OAuthAuthenticationFailedException()
        } catch (_: OAuthAuthenticationFailedException) {
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_INVALID)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load Kakao user info with native access token" }
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_INVALID)
        }

    private data class MinimalKakaoAuthAttributes(
        private val externalId: String,
    ) : OAuthAttributes {
        override fun getExternalId(): String = externalId

        override fun getProvider(): OAuthProvider = OAuthProvider.KAKAO

        override fun getEmail(): String = ""

        override fun getName(): String = ""
    }

    companion object {
        private const val KAKAO_PROVIDER_ID = "KAKAO"
    }
}
