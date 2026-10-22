package core.application.security.oauth.token

import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

    @ParameterizedTest
    @CsvSource(".example.com,true", ".example.com,false", "localhost,true", "localhost,false", "'',true", "'',false")
    fun `invalidate access token expires the issued cookie with matching attributes`(
        cookieDomain: String,
        secure: Boolean,
    ) {
        val injector = createInjector(cookieDomain, secure)
        val issued = MockHttpServletResponse()
        val expired = MockHttpServletResponse()

        injector.injectAccessToken("access-token", issued)
        injector.invalidateAccessToken(expired)

        val issuedCookie = requireNotNull(issued.getCookie("accessToken"))
        val expiredCookie = requireNotNull(expired.getCookie("accessToken"))
        assertThat(expiredCookie.value).isEmpty()
        assertThat(expiredCookie.maxAge).isZero()
        assertThat(expiredCookie.domain).isEqualTo(issuedCookie.domain)
        assertThat(expiredCookie.domain).isEqualTo(if (cookieDomain == ".example.com") "example.com" else null)
        assertThat(expiredCookie.path).isEqualTo(issuedCookie.path).isEqualTo("/")
        assertThat(expiredCookie.secure).isEqualTo(issuedCookie.secure).isEqualTo(secure)
        assertThat(expiredCookie.isHttpOnly).isEqualTo(issuedCookie.isHttpOnly).isTrue()
        assertThat(expiredCookie.getAttribute("SameSite")).isEqualTo(issuedCookie.getAttribute("SameSite")).isEqualTo("Lax")
        assertThat(expired.getHeader("Set-Cookie")).contains("Max-Age=0", "Expires=")
    }

    private fun createInjector(
        cookieDomain: String,
        secure: Boolean = true,
    ): JwtTokenInjector =
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
                            secure = secure,
                        ),
                ),
        )
}
