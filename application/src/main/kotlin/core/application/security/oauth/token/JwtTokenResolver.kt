package core.application.security.oauth.token

import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class JwtTokenResolver {
    fun resolveRefreshTokenFromRequest(request: HttpServletRequest): String? =
        resolveFromCookie(request, REFRESH_TOKEN_CAMEL_CASE)
            ?: resolveFromBearerHeader(request.getHeader(HttpHeaders.AUTHORIZATION))

    private fun resolveFromCookie(
        request: HttpServletRequest,
        cookieName: String,
    ): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value

    private fun resolveFromBearerHeader(authorizationHeader: String?): String? {
        if (authorizationHeader.isNullOrBlank()) {
            return null
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null
        }

        return authorizationHeader
            .removePrefix(BEARER_PREFIX)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
