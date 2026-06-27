package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.presentation.response.KakaoNativeLoginResponse
import core.application.security.oauth.exception.OAuthAuthenticationFailedException
import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class KakaoNativeLoginService(
    private val kakaoUserInfoClient: KakaoUserInfoClient,
    private val handleMemberLoginUseCase: HandleMemberLoginUseCase,
    private val jwtTokenProvider: JwtTokenProvider,
    private val memberPersistencePort: MemberPersistencePort,
) {
    private val logger = KotlinLogging.logger { }

    fun login(kakaoAccessToken: String): KakaoNativeLoginResponse {
        val loginAttributes = resolveAttributes(kakaoAccessToken)
        val loginResult =
            handleMemberLoginUseCase.handleLoginSuccess(
                loginAttributes,
            )
        val refreshToken =
            loginResult.refreshToken
                ?: throw MemberDeletedException()
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
            val attributes = kakaoUserInfoClient.getUserAttributesByAccessToken(kakaoAccessToken)
            OAuthAttributes.of(KAKAO_PROVIDER_ID, attributes)
                ?: throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_INVALID)
        } catch (_: OAuthAuthenticationFailedException) {
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_INVALID)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load Kakao user info with native access token" }
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_INVALID)
        }

    companion object {
        private const val KAKAO_PROVIDER_ID = "KAKAO"
    }
}
