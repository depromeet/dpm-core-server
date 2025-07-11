package com.server.dpmcore.security.oauth.dto

import com.server.dpmcore.member.memberOAuth.domain.OAuthProvider

interface OAuthAttributes {
    fun getExternalId(): String

    fun getProvider(): OAuthProvider

    fun getEmail(): String

    companion object {
        fun of(providerId: String, attributes: Map<String, Any>): OAuthAttributes {
            return when {
                OAuthProvider.KAKAO.isProviderOf(providerId) -> KakaoAuthAttributes.of(attributes)
                else -> throw IllegalArgumentException("Unsupported provider: $providerId")
            }
        }
    }
}
