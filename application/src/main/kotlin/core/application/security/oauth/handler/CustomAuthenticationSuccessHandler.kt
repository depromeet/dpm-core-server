package core.application.security.oauth.handler

import core.application.member.application.exception.MemberDeletedException
import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
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
            try {
                handleMemberLoginUseCase.handleLoginSuccess(oAuth2User.authAttributes, deviceId)
            } catch (exception: MemberDeletedException) {
                redirectFailure(request, response, exception.getCode().getCode())
                return
            }

        val refreshToken = loginResult.refreshToken
        if (refreshToken == null) {
            redirectFailure(request, response, OAuthExceptionCode.AUTHENTICATION_FAILED.getCode())
            return
        }
        tokenInjector.injectRefreshToken(refreshToken, response)

        val redirectUri = oAuthCallbackRedirectService.buildSuccessRedirectUri(request)
        oAuthCallbackRedirectService.clearClientRedirectUri(response)
        response.sendRedirect(redirectUri)
    }

    private fun redirectFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        errorCode: String,
    ) {
        SecurityContextHolder.clearContext()
        request.getSession(false)?.invalidate()
        tokenInjector.invalidateRefreshToken(response)
        val redirectUri = oAuthCallbackRedirectService.buildFailureRedirectUri(request, errorCode)
        oAuthCallbackRedirectService.clearClientRedirectUri(response)
        response.sendRedirect(redirectUri)
    }
}
