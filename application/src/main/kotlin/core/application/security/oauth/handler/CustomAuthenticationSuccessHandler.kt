package core.application.security.oauth.handler

import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationSuccessHandler(
    private val tokenInjector: JwtTokenInjector,
    private val handleMemberLoginUseCase: HandleMemberLoginUseCase,
) : AuthenticationSuccessHandler {
    /**
     * 소셜 로그인 인증 성공 시 호출되며, 인증 정보를 기반으로 멤버 로그인 처리를 수행한 후
     * 리프레시 토큰을 응답 쿠키에 추가하고 단일 프론트엔드 리다이렉트 URL로 이동시킴.
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as CustomOAuth2User

        val loginResult =
            handleMemberLoginUseCase.handleLoginSuccess(oAuth2User.authAttributes)

        loginResult.refreshToken?.let {
            tokenInjector.injectRefreshToken(it, response)
        }

        response.sendRedirect(loginResult.redirectUrl)
    }
}
