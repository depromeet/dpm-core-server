package core.application.security.oauth.token

import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.refreshToken.aggregate.RefreshToken
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class JwtTokenInjector(
    private val tokenProperties: TokenProperties,
    private val securityProperties: SecurityProperties,
) {
    fun injectRefreshToken(
        refreshToken: RefreshToken,
        response: HttpServletResponse,
    ) {
        val cookie =
            "${REFRESH_TOKEN_CAMEL_CASE}=${refreshToken.token}; " +
                "Path=/; " +
                "Domain=${securityProperties.cookie.domain}; " +
                "Max-Age=${tokenProperties.expirationTime.refreshToken}; " +
                "HttpOnly; Secure; SameSite=None"

        response.addHeader("Set-Cookie", cookie)
    }

    fun invalidateRefreshToken(response: HttpServletResponse) {
        val cookie =
            "${REFRESH_TOKEN_CAMEL_CASE}=; " +
                "Path=/; " +
                "Domain=${securityProperties.cookie.domain}; " +
                "Max-Age=0; " +
                "HttpOnly; Secure; SameSite=None"

        response.addHeader("Set-Cookie", cookie)
    }
}
