package com.server.dpmcore.security.oauth.token

import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.properties.TokenProperties
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
                domain = securityProperties.cookie.domain
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
                domain = securityProperties.cookie.domain
                secure = securityProperties.cookie.secure
                setAttribute("SameSite", "None")
            }
        response.addCookie(cookie)
    }
}
