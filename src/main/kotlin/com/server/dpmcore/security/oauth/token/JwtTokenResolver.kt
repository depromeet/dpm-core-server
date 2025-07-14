package com.server.dpmcore.security.oauth.token

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
import org.springframework.stereotype.Component

@Component
class JwtTokenResolver {

    fun resolveRefreshTokenFromRequest(request: HttpServletRequest): String? = resolveFromCookie(request, REFRESH_TOKEN)

    private fun resolveFromCookie(request: HttpServletRequest, cookieName: String): String? {
        return request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
    }
}
