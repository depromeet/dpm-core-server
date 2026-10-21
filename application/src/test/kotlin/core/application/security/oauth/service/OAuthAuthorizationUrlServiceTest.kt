package core.application.security.oauth.service

import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.util.UriComponentsBuilder

class OAuthAuthorizationUrlServiceTest {
    @Test
    fun `Apple 등록 URL에 응답 모드가 있어도 연결 URL에는 한 번만 포함한다`() {
        val redirect = "https://example.com/callback"
        val registration =
            ClientRegistration.withRegistrationId("apple")
                .clientId("test-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirect)
                .authorizationUri("https://appleid.apple.com/auth/authorize?response_mode=form_post")
                .tokenUri("https://appleid.apple.com/auth/token")
                .scope("name", "email")
                .build()
        val validator = mock(OAuthRedirectUriValidator::class.java)
        `when`(validator.validate(redirect)).thenReturn(redirect)
        val service = OAuthAuthorizationUrlService(InMemoryClientRegistrationRepository(registration), validator)

        val result = service.buildLinkAuthorizationUrl("APPLE", redirect, "state", "nonce", "challenge")
        val params = UriComponentsBuilder.fromUriString(result.authorizationUrl).build().queryParams

        assertThat(params["response_mode"]).containsExactly("form_post")
        assertThat(params["nonce"]).containsExactly("nonce")
        assertThat(params["state"]).containsExactly("state")
        assertThat(result.redirectUri).isEqualTo(redirect)
    }
}
