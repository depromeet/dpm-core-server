package core.application.security.oauth.token

import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import kotlin.math.log

@Component
class JwtTokenInjector(
    private val tokenProperties: TokenProperties,
    private val securityProperties: SecurityProperties,
) {
    fun injectRefreshToken(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        val domain = securityProperties.cookie.domain
        System.out.println("$domain //////////// $refreshToken")
        response.addHeader(
            "Set-Cookie",
            "refresh_token=$refreshToken; " +
                    "Path=/; " +
                    "Domain=$domain; " +
                    "Max-Age=${60 * 60 * 24 * 14}; " +
                    "Secure; HttpOnly; SameSite=None"
        )
    }

    private fun setSameSite(cookie: Cookie, sameSite: String) {
        // Servlet API는 SameSite 직접 지원 안 함 → 헤더로 강제
        cookie.setAttribute("SameSite", sameSite)
    }



/* Temp deprecate 20260114
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
*/

/* Temp deprecate 20260114
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
        }*/

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
