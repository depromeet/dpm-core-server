package core.domain.security.oauth.dto

import core.domain.member.enums.OAuthProvider

interface OAuthAttributes {
    fun getExternalId(): String

    fun getProvider(): OAuthProvider

    fun getEmail(): String

    fun getName(): String

    companion object {
        fun of(
            providerId: String,
            attributes: Map<String, Any>,
        ): OAuthAttributes? =
            when {
                OAuthProvider.KAKAO.isProviderOf(providerId) -> KakaoAuthAttributes.of(attributes)
                else -> null
            }
    }
}
