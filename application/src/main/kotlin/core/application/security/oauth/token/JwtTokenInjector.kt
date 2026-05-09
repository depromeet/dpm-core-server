package core.application.security.oauth.token

import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.refreshToken.aggregate.RefreshToken
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class JwtTokenInjector(
    private val tokenProperties: TokenProperties,
    private val securityProperties: SecurityProperties,
) {
    fun injectAccessToken(
        accessToken: String,
        response: HttpServletResponse,
    ) {
        response.addHeader(
            SET_COOKIE_HEADER,
            buildCookie(
                name = ACCESS_TOKEN_CAMEL_CASE,
                value = accessToken,
                maxAgeSeconds = tokenProperties.expirationTime.accessToken,
                secure = securityProperties.cookie.secure,
                sameSite = SAME_SITE_LAX,
            ),
        )
    }

    fun injectRefreshToken(
        refreshToken: RefreshToken,
        response: HttpServletResponse,
    ) = injectRefreshToken(refreshToken.token, response)

    fun injectRefreshToken(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        response.addHeader(
            SET_COOKIE_HEADER,
            buildCookie(
                name = REFRESH_TOKEN_CAMEL_CASE,
                value = refreshToken,
                maxAgeSeconds = tokenProperties.expirationTime.refreshToken,
                secure = true,
                sameSite = SAME_SITE_NONE,
            ),
        )
    }

    fun invalidateRefreshToken(response: HttpServletResponse) {
        response.addHeader(
            SET_COOKIE_HEADER,
            buildCookie(
                name = REFRESH_TOKEN_CAMEL_CASE,
                value = "",
                maxAgeSeconds = 0,
                secure = true,
                sameSite = SAME_SITE_NONE,
            ),
        )
    }

    private fun buildCookie(
        name: String,
        value: String,
        maxAgeSeconds: Long,
        secure: Boolean,
        sameSite: String,
    ): String {
        val builder =
            ResponseCookie
                .from(name, value)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)

        resolveCookieDomain()?.let(builder::domain)

        return builder.build().toString()
    }

    private fun resolveCookieDomain(): String? {
        val configuredDomain = securityProperties.cookie.domain.trim()
        if (configuredDomain.isBlank() || configuredDomain.equals("localhost", ignoreCase = true)) {
            return null
        }

        return configuredDomain.removePrefix(".")
    }

    companion object {
        private const val SET_COOKIE_HEADER = "Set-Cookie"
        private const val ACCESS_TOKEN_CAMEL_CASE = "accessToken"
        private const val SAME_SITE_LAX = "Lax"
        private const val SAME_SITE_NONE = "None"
    }
}
