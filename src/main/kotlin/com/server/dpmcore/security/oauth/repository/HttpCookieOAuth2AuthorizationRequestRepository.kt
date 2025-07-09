package com.server.dpmcore.security.oauth.repository

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component

@Component
class HttpCookieOAuth2AuthorizationRequestRepository(
    private val authorizationRequestCookieValueMapper: AuthorizationRequestCookieValueMapper,
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private val REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST"
    private val REQUEST_COOKIE_MAX_AGE = 180

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        return getAuthorizationRequestCookie(request)
            ?.value
            ?.let { authorizationRequestCookieValueMapper.deserialize(it) }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (authorizationRequest == null) {
            deleteCookie(request, response)
            return
        }

        addCookie(
            response = response,
            value = authorizationRequestCookieValueMapper.serialize(authorizationRequest)
        )
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? {
        val authRequest = loadAuthorizationRequest(request)
        deleteCookie(request, response)
        return authRequest
    }

    private fun addCookie(response: HttpServletResponse, value: String) {
        val cookie = Cookie(REQUEST_COOKIE_NAME, value).apply {
            path = "/"
            isHttpOnly = true
            maxAge = REQUEST_COOKIE_MAX_AGE
        }
        response.addCookie(cookie)
    }

    private fun deleteCookie(request: HttpServletRequest, response: HttpServletResponse) {
        request.cookies?.firstOrNull { it.name == REQUEST_COOKIE_NAME }?.let {
            val cookie = Cookie(REQUEST_COOKIE_NAME, "").apply {
                path = "/"
                maxAge = 0
            }
            response.addCookie(cookie)
        }
    }

    private fun getAuthorizationRequestCookie(request: HttpServletRequest): Cookie? {
        return request.cookies?.firstOrNull { it.name == REQUEST_COOKIE_NAME }
    }
}
