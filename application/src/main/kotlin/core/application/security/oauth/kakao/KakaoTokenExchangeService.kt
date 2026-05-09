package core.application.security.oauth.kakao

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Service
class KakaoTokenExchangeService(
    private val clientRegistrationRepository: ClientRegistrationRepository,
) {
    private val restClient = RestClient.create()

    fun getUserAttributes(
        authorizationCode: String,
        redirectUri: String?,
    ): Map<String, Any> {
        val registration =
            clientRegistrationRepository.findByRegistrationId(KAKAO_REGISTRATION_ID)
                ?: throw IllegalStateException("Kakao OAuth registration is missing")
        val tokenResponse =
            exchangeToken(
                authorizationCode = authorizationCode,
                redirectUri = redirectUri ?: registration.redirectUri,
                clientId = registration.clientId,
                clientSecret = registration.clientSecret,
                tokenUri = registration.providerDetails.tokenUri,
            )

        return getUserInfo(
            accessToken = tokenResponse.accessToken,
            userInfoUri = registration.providerDetails.userInfoEndpoint.uri,
        )
    }

    private fun exchangeToken(
        authorizationCode: String,
        redirectUri: String,
        clientId: String,
        clientSecret: String?,
        tokenUri: String,
    ): KakaoTokenResponse {
        val formData =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", clientId)
                clientSecret?.takeIf { it.isNotBlank() }?.let {
                    add("client_secret", it)
                }
                add("redirect_uri", redirectUri)
                add("code", authorizationCode)
            }

        return restClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(KakaoTokenResponse::class.java)
            ?: throw IllegalStateException("Failed to retrieve tokens from Kakao")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getUserInfo(
        accessToken: String,
        userInfoUri: String,
    ): Map<String, Any> =
        restClient.get()
            .uri(userInfoUri)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(Map::class.java)
            as? Map<String, Any>
            ?: throw IllegalStateException("Failed to retrieve Kakao user info")

    data class KakaoTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("refresh_token")
        val refreshToken: String? = null,
        @JsonProperty("expires_in")
        val expiresIn: Int? = null,
        val scope: String? = null,
        @JsonProperty("refresh_token_expires_in")
        val refreshTokenExpiresIn: Int? = null,
    )

    companion object {
        private const val KAKAO_REGISTRATION_ID = "kakao"
    }
}
