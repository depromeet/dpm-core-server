package core.application.security.oauth.token

import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.refreshToken.aggregate.RefreshToken
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
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
        addCookie(
            OAuth2ParameterNames.REFRESH_TOKEN,
            refreshToken.token,
            tokenProperties.expirationTime.refreshToken.toInt(),
            response,
        )
    }

    fun addCookie(
        name: String,
        value: String,
        maxAge: Int,
        response: HttpServletResponse,
    ) {
        val cookie =
            Cookie(name, value).apply {
                path = "/"
                this.maxAge = maxAge
                isHttpOnly = securityProperties.cookie.httpOnly
                domain = null // Host Only
                secure = securityProperties.cookie.secure
                setAttribute("SameSite", "None")
            }
        response.addCookie(cookie)
    }

    fun invalidateCookie(
        name: String,
        response: HttpServletResponse,
    ) {
        val cookie =
            Cookie(name, null).apply {
                path = "/"
                this.maxAge = 0
                isHttpOnly = securityProperties.cookie.httpOnly
                domain = null // Host Only
                secure = securityProperties.cookie.secure
                setAttribute("SameSite", "None")
            }
        response.addCookie(cookie)
    }
}
