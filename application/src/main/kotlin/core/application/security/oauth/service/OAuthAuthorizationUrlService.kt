package core.application.security.oauth.service

import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.security.SecureRandom
import java.util.Base64

@Service
class OAuthAuthorizationUrlService(
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val redirectUriValidator: OAuthRedirectUriValidator,
) {
    fun buildAuthorizationUrl(
        provider: String,
        redirectUri: String,
        state: String?,
    ): OAuthAuthorizationUrl {
        val registrationId = provider.lowercase()
        val registration =
            clientRegistrationRepository.findByRegistrationId(registrationId)
                ?: throw IllegalArgumentException("Unsupported OAuth provider: $provider")
        val resolvedRedirectUri = redirectUriValidator.validate(redirectUri)
        val resolvedState = state?.takeIf { it.isNotBlank() } ?: generateState()

        val authorizationUrl =
            UriComponentsBuilder
                .fromUriString(registration.providerDetails.authorizationUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.clientId)
                .queryParam("redirect_uri", resolvedRedirectUri)
                .queryParam("scope", registration.scopes.joinToString(" "))
                .queryParam("state", resolvedState)
                .build()
                .encode()
                .toUriString()

        return OAuthAuthorizationUrl(
            authorizationUrl = authorizationUrl,
            state = resolvedState,
            redirectUri = resolvedRedirectUri,
        )
    }

    private fun generateState(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private val secureRandom = SecureRandom()
    }
}

data class OAuthAuthorizationUrl(
    val authorizationUrl: String,
    val state: String,
    val redirectUri: String,
)
