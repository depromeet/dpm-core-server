package core.application.security.oauth.repository

import core.application.security.oauth.repository.mapper.AuthorizationRequestCookieValueMapper
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
        val cookieValue =
            "$REQUEST_COOKIE_NAME=$value; " +
                    "Path=/; " +
                    "HttpOnly; " +
                    "Secure; " +
                    "SameSite=None; " +
                    "Max-Age=$REQUEST_COOKIE_MAX_AGE"

        response.addHeader("Set-Cookie", cookieValue)
    }
    private fun deleteCookie(response: HttpServletResponse) {
        val cookieValue =
            "$REQUEST_COOKIE_NAME=; Path=/; Max-Age=0; Secure; SameSite=None"

        response.addHeader("Set-Cookie", cookieValue)
    }

    private fun getAuthorizationRequestCookie(request: HttpServletRequest): Cookie? =
        request.cookies?.firstOrNull {
            it.name == REQUEST_COOKIE_NAME
        }
}
