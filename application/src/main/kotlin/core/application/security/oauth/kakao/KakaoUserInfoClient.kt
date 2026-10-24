package core.application.security.oauth.kakao

interface KakaoUserInfoClient {
    fun getAccessTokenInfo(accessToken: String): KakaoAccessTokenInfo

    fun getUserAttributes(
        authorizationCode: String,
        redirectUri: String?,
    ): Map<String, Any>

    fun getServiceUserIdByAccessToken(accessToken: String): Long

    fun getUserAttributesByAccessToken(accessToken: String): Map<String, Any>
}

data class KakaoAccessTokenInfo(
    val id: Long,
    @com.fasterxml.jackson.annotation.JsonProperty("app_id") val appId: Long,
    @com.fasterxml.jackson.annotation.JsonProperty("expires_in") val expiresIn: Long,
)
