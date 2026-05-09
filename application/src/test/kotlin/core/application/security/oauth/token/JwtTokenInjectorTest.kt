package core.application.security.oauth.token

import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletResponse

class JwtTokenInjectorTest {
    @Test
    fun `inject refresh token omits localhost domain`() {
        val response = MockHttpServletResponse()
        val injector = createInjector(cookieDomain = "localhost")

        injector.injectRefreshToken("refresh-token", response)

        val cookieHeader = response.getHeader("Set-Cookie")
        assertThat(cookieHeader).contains("refreshToken=refresh-token")
        assertThat(cookieHeader).contains("Path=/")
        assertThat(cookieHeader).contains("SameSite=None")
        assertThat(cookieHeader).doesNotContain("Domain=")
    }

    @Test
    fun `inject access token strips leading dot from configured domain`() {
        val response = MockHttpServletResponse()
        val injector = createInjector(cookieDomain = ".depromeet.shop")

        injector.injectAccessToken("access-token", response)

        val cookieHeader = response.getHeader("Set-Cookie")
        assertThat(cookieHeader).contains("accessToken=access-token")
        assertThat(cookieHeader).contains("Domain=depromeet.shop")
        assertThat(cookieHeader).doesNotContain("Domain=.depromeet.shop")
        assertThat(cookieHeader).contains("SameSite=Lax")
    }

    @Test
    fun `invalidate refresh token keeps normalized domain`() {
        val response = MockHttpServletResponse()
        val injector = createInjector(cookieDomain = ".depromeet.com")

        injector.invalidateRefreshToken(response)

        val cookieHeader = response.getHeader("Set-Cookie")
        assertThat(cookieHeader).contains("refreshToken=")
        assertThat(cookieHeader).contains("Domain=depromeet.com")
        assertThat(cookieHeader).contains("Max-Age=0")
    }

    private fun createInjector(cookieDomain: String): JwtTokenInjector =
        JwtTokenInjector(
            tokenProperties =
                TokenProperties(
                    secretKey = "secret",
                    expirationTime =
                        TokenProperties.ExpirationTime(
                            accessToken = 60 * 60 * 24L,
                            refreshToken = 60 * 60 * 24 * 30L,
                        ),
                ),
            securityProperties =
                SecurityProperties(
                    logoutUrl = "/logout",
                    cookie =
                        SecurityProperties.Cookie(
                            domain = cookieDomain,
                            httpOnly = true,
                            secure = true,
                        ),
                ),
        )
}
