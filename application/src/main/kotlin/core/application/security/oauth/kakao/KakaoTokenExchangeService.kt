package core.application.security.oauth.kakao

import org.springframework.http.HttpHeaders
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

    fun getUser(authorizationCode: String): KakaoUser {
        val registration =
            clientRegistrationRepository.findByRegistrationId("kakao")
                ?: throw IllegalStateException("Kakao client registration not found")

        val formData =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", registration.clientId)
                if (registration.clientSecret.isNotBlank()) {
                    add("client_secret", registration.clientSecret)
                }
                add("redirect_uri", registration.redirectUri)
                add("code", authorizationCode)
            }

        val token =
            restClient.post()
                .uri(registration.providerDetails.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KakaoTokenResponse::class.java)
                ?: throw IllegalStateException("Failed to retrieve tokens from Kakao")

        val attributes =
            restClient.get()
                .uri(registration.providerDetails.userInfoEndpoint.uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${token.access_token}")
                .retrieve()
                .body(Map::class.java)
                ?: throw IllegalStateException("Failed to retrieve user from Kakao")

        val externalId = attributes["id"]?.toString() ?: throw IllegalStateException("Kakao id missing")
        val kakaoAccount =
            attributes["kakao_account"] as? Map<*, *>
                ?: throw IllegalStateException("Kakao account missing")
        val email = kakaoAccount["email"] as? String ?: throw IllegalStateException("Kakao email missing")
        val profile = kakaoAccount["profile"] as? Map<*, *>
        val name = profile?.get("nickname") as? String ?: ""

        return KakaoUser(
            externalId = externalId,
            email = email,
            name = name,
        )
    }

    data class KakaoTokenResponse(
        val access_token: String,
        val token_type: String,
        val refresh_token: String? = null,
        val expires_in: Int? = null,
        val scope: String? = null,
        val refresh_token_expires_in: Int? = null,
    )

    data class KakaoUser(
        val externalId: String,
        val email: String,
        val name: String,
    )
}
