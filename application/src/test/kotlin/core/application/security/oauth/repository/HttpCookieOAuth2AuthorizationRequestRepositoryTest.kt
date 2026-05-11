package core.application.security.oauth.repository

import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.repository.mapper.AuthorizationRequestCookieValueMapper
import core.application.security.properties.CorsProperties
import core.application.security.properties.SecurityProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {
    @Test
    fun `save authorization request stores browser redirect cookie for direct oauth entry`() {
        val repository = createRepository()
        val request =
            MockHttpServletRequest("GET", "/oauth2/authorization/kakao").apply {
                addHeader("Referer", "https://admin.depromeet.com/login")
            }
        val response = MockHttpServletResponse()

        repository.saveAuthorizationRequest(createAuthorizationRequest(), request, response)

        val cookies = response.getHeaders("Set-Cookie")
        assertThat(cookies).anyMatch { it.startsWith("OAUTH2_AUTH_REQUEST=") }
        assertThat(cookies).anyMatch { it.startsWith("OAUTH2_REDIRECT_URI=") }
    }

    private fun createRepository(): HttpCookieOAuth2AuthorizationRequestRepository =
        HttpCookieOAuth2AuthorizationRequestRepository(
            authorizationRequestCookieValueMapper = AuthorizationRequestCookieValueMapper(),
            oAuthCallbackRedirectService =
                OAuthCallbackRedirectService(
                    redirectUriValidator =
                        OAuthRedirectUriValidator(
                            CorsProperties(
                                allowedOrigins =
                                    listOf(
                                        "https://core.depromeet.com",
                                        "https://admin.depromeet.com",
                                        "https://api.depromeet.com",
                                        "https://kauth.kakao.com",
                                        "https://appleid.apple.com",
                                    ),
                            ),
                        ),
                    securityProperties =
                        SecurityProperties(
                            logoutUrl = "/logout",
                            cookie =
                                SecurityProperties.Cookie(
                                    domain = "depromeet.com",
                                    httpOnly = true,
                                    secure = true,
                                ),
                        ),
                ),
        )

    private fun createAuthorizationRequest(): OAuth2AuthorizationRequest =
        OAuth2AuthorizationRequest
            .authorizationCode()
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .clientId("client-id")
            .redirectUri("https://api.depromeet.com/login/oauth2/code/kakao")
            .state("state")
            .build()
}
