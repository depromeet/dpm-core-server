package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.presentation.response.KakaoNativeLoginResponse
import core.application.security.oauth.exception.OAuthAuthenticationFailedException
import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class KakaoNativeLoginService(
    private val kakaoUserInfoClient: KakaoUserInfoClient,
    private val handleMemberLoginUseCase: HandleMemberLoginUseCase,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenInjector: JwtTokenInjector,
    private val deviceIdResolver: DeviceIdResolver,
    private val memberPersistencePort: MemberPersistencePort,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * 카카오 네이티브 로그인.
     *
     * 이 경로는 이전까지 body 로만 토큰을 반환하고 Set-Cookie 를 내리지 않았다.
     * 그 결과 앱(WebView)이 심는 host-only 쿠키만 남았고, 그 쿠키는 API 서브도메인으로
     * 전송되지 않아 재발급 요청에 리프레시 토큰이 아예 실리지 않았다.
     * 여기서 서버가 Domain 범위 쿠키를 직접 심어 그 경로를 복구한다.
     */
    fun login(
        kakaoAccessToken: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): KakaoNativeLoginResponse {
        val loginAttributes = resolveAttributes(kakaoAccessToken)
        val deviceId = deviceIdResolver.resolve(request, response)

        val loginResult = handleMemberLoginUseCase.handleLoginSuccess(loginAttributes, deviceId)
        val refreshToken = loginResult.refreshToken ?: throw MemberDeletedException()

        val memberId = refreshToken.memberId.value
        val accessToken = jwtTokenProvider.generateAccessToken(memberId.toString())
        val memberStatus = memberPersistencePort.findById(refreshToken.memberId)?.status

        jwtTokenInjector.injectAccessToken(accessToken, response)
        jwtTokenInjector.injectRefreshToken(refreshToken, response)

        logger.info { "Kakao native login succeeded for memberId=$memberId" }

        return KakaoNativeLoginResponse.loginSuccess(
            accessToken = accessToken,
            refreshToken = refreshToken.requirePlainToken(),
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
