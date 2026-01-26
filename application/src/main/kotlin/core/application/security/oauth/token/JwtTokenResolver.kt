package core.application.security.oauth.token

import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class JwtTokenResolver {
    fun resolveRefreshTokenFromRequest(request: HttpServletRequest): String? =
        resolveFromCookie(request, REFRESH_TOKEN_CAMEL_CASE)

    private fun resolveFromCookie(
        request: HttpServletRequest,
        cookieName: String,
    ): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
}
