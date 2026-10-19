package core.application.security.oauth.repository

import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import core.application.security.oauth.repository.mapper.AuthorizationRequestCookieValueMapper
import core.application.security.properties.SecurityProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component

private const val REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST"
private const val REQUEST_COOKIE_MAX_AGE = 180

@Component
class HttpCookieOAuth2AuthorizationRequestRepository(
    private val authorizationRequestCookieValueMapper: AuthorizationRequestCookieValueMapper,
    private val oAuthCallbackRedirectService: OAuthCallbackRedirectService,
    private val securityProperties: SecurityProperties,
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        getAuthorizationRequestCookie(request)
            ?.value
            ?.let { authorizationRequestCookieValueMapper.deserialize(it) }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            deleteCookie(response)
            return
        }

        addCookie(
            response = response,
            value = authorizationRequestCookieValueMapper.serialize(authorizationRequest),
        )
        oAuthCallbackRedirectService.rememberClientRedirectUri(request, response)
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val authRequest = loadAuthorizationRequest(request)
        deleteCookie(response)
        return authRequest
    }

    private fun addCookie(
        response: HttpServletResponse,
        value: String,
    ) {
        response.addHeader("Set-Cookie", buildCookieHeader(value = value, maxAge = REQUEST_COOKIE_MAX_AGE))
    }

    private fun deleteCookie(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", buildCookieHeader(value = "", maxAge = 0))
    }

    private fun buildCookieHeader(
        value: String,
        maxAge: Int,
    ): String {
        val secure = securityProperties.cookie.secure
        // SameSite=None requires Secure; use Lax on local HTTP so browsers keep the cookie.
        val sameSite = if (secure) "None" else "Lax"
        val secureAttr = if (secure) "; Secure" else ""

        return "$REQUEST_COOKIE_NAME=$value; Path=/; HttpOnly$secureAttr; SameSite=$sameSite; Max-Age=$maxAge"
    }

    private fun getAuthorizationRequestCookie(request: HttpServletRequest): Cookie? =
        request.cookies?.firstOrNull {
            it.name == REQUEST_COOKIE_NAME
        }
}
