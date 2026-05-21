package core.application.security.oauth.kakao

interface KakaoUserInfoClient {
    fun getUserAttributes(
        authorizationCode: String,
        redirectUri: String?,
    ): Map<String, Any>

    fun getUserAttributesByAccessToken(accessToken: String): Map<String, Any>
}
