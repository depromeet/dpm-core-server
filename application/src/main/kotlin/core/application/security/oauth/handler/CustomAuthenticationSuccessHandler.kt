package core.application.security.oauth.handler

import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import core.application.security.oauth.token.DeviceIdResolver
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
    private val oAuthCallbackRedirectService: OAuthCallbackRedirectService,
    private val deviceIdResolver: DeviceIdResolver,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as CustomOAuth2User
        val deviceId = deviceIdResolver.resolve(request, response)

        val loginResult =
            handleMemberLoginUseCase.handleLoginSuccess(oAuth2User.authAttributes, deviceId)

        loginResult.refreshToken?.let {
            tokenInjector.injectRefreshToken(it, response)
        }

        val redirectUri = oAuthCallbackRedirectService.buildSuccessRedirectUri(request)
        oAuthCallbackRedirectService.clearClientRedirectUri(response)
        response.sendRedirect(redirectUri)
    }
}
