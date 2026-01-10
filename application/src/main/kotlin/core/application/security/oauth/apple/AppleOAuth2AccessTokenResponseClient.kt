package core.application.security.oauth.apple

import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.stereotype.Component

@Component
class AppleOAuth2AccessTokenResponseClient(
    private val appleClientSecretGenerator: AppleClientSecretGenerator
) : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private val defaultClient = RestClientAuthorizationCodeTokenResponseClient()

    override fun getTokenResponse(authorizationGrantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse {
        val clientRegistration = authorizationGrantRequest.clientRegistration

        // Only intercept Apple requests to inject dynamic client_secret
        if (clientRegistration.registrationId.equals("apple", ignoreCase = true)) {
            val secret = appleClientSecretGenerator.generateClientSecret()

            // Create a new ClientRegistration with the dynamic secret
            val newClientRegistration = ClientRegistration
                .withClientRegistration(clientRegistration)
                .clientSecret(secret)
                .build()

            // Create a new request with the updated registration
            val newRequest = OAuth2AuthorizationCodeGrantRequest(
                newClientRegistration,
                authorizationGrantRequest.authorizationExchange
            )

            return defaultClient.getTokenResponse(newRequest)
        }

        return defaultClient.getTokenResponse(authorizationGrantRequest)
    }
}
