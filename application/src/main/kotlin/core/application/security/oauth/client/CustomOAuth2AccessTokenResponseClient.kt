package core.application.security.oauth.client

import core.application.security.oauth.apple.AppleClientSecretGenerator
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.stereotype.Component

@Component
class CustomOAuth2AccessTokenResponseClient(
    private val appleClientSecretGenerator: AppleClientSecretGenerator?,
) : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
    private val defaultClient = RestClientAuthorizationCodeTokenResponseClient()

    override fun getTokenResponse(
        authorizationGrantRequest: OAuth2AuthorizationCodeGrantRequest,
    ): OAuth2AccessTokenResponse {
        val clientRegistration = authorizationGrantRequest.clientRegistration

        // Route logic based on Provider
        if (clientRegistration.registrationId.equals("apple", ignoreCase = true)) {
            return getAppleTokenResponse(authorizationGrantRequest, clientRegistration)
        }

        // Default behavior for others (e.g. Kakao)
        return defaultClient.getTokenResponse(authorizationGrantRequest)
    }

    private fun getAppleTokenResponse(
        request: OAuth2AuthorizationCodeGrantRequest,
        registration: ClientRegistration,
    ): OAuth2AccessTokenResponse {
        val generator = appleClientSecretGenerator
            ?: throw IllegalStateException("Apple OAuth is not configured in this environment")
        val secret = generator.generateClientSecret()

        // Create a new ClientRegistration with the dynamic secret
        val newClientRegistration =
            ClientRegistration
                .withClientRegistration(registration)
                .clientSecret(secret)
                .build()

        // Create a new request with the updated registration
        val newRequest =
            OAuth2AuthorizationCodeGrantRequest(
                newClientRegistration,
                request.authorizationExchange,
            )

        return defaultClient.getTokenResponse(newRequest)
    }
}
