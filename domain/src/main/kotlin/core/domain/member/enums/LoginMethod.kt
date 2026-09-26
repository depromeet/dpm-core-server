package core.domain.member.enums

/**
 * 회원이 이번 세션에 사용한 로그인 수단입니다.
 * 로그인 시 토큰 클레임에 담기고, 토큰 재발급 시 그대로 이어집니다.
 */
enum class LoginMethod {
    KAKAO,
    APPLE,
    EMAIL,
    ;

    companion object {
        fun from(provider: OAuthProvider): LoginMethod =
            when (provider) {
                OAuthProvider.KAKAO -> KAKAO
                OAuthProvider.APPLE -> APPLE
            }

        fun fromOrNull(value: String?): LoginMethod? = entries.firstOrNull { it.name == value }
    }
}
