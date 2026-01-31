package core.application.security.oauth.apple

import core.application.security.properties.AppleProperties
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Service
@Profile("!local")
class AppleTokenExchangeService(
    private val appleClientSecretGenerator: AppleClientSecretGenerator,
    private val appleProperties: AppleProperties,
) {
    private val restClient = RestClient.create()

    fun getTokens(authorizationCode: String): AppleTokenResponse {
        val clientSecret = appleClientSecretGenerator.generateClientSecret()

        val formData =
            LinkedMultiValueMap<String, String>().apply {
                add("client_id", appleProperties.clientId)
                add("client_secret", clientSecret)
                add("code", authorizationCode)
                add("grant_type", "authorization_code")
                add("redirect_uri", appleProperties.redirectUri)
            }

        return restClient
            .post()
            .uri("https://appleid.apple.com/auth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(AppleTokenResponse::class.java)
            ?: throw RuntimeException("Failed to retrieve tokens from Apple")
    }

    data class AppleTokenResponse(
        val access_token: String,
        val token_type: String,
        val expires_in: Int,
        val refresh_token: String?,
        val id_token: String,
    )
}
