package core.application.security.oauth.handler

import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationFailureHandler(
    private val oAuthCallbackRedirectService: OAuthCallbackRedirectService,
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?,
    ) {
        response ?: return
        val requestToHandle = request ?: return
        val redirectUri =
            oAuthCallbackRedirectService.buildFailureRedirectUri(
                requestToHandle,
                OAuthExceptionCode.AUTHENTICATION_FAILED.getCode(),
            )
        oAuthCallbackRedirectService.clearClientRedirectUri(response)
        response.sendRedirect(redirectUri)
    }
}
