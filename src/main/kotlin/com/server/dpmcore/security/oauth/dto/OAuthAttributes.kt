package com.server.dpmcore.security.oauth.dto

import com.server.dpmcore.member.memberOAuth.domain.model.OAuthProvider
import com.server.dpmcore.security.oauth.exception.UnsupportedOAuthProviderException

interface OAuthAttributes {
    fun getExternalId(): String

    fun getProvider(): OAuthProvider

    fun getEmail(): String

    fun getName(): String

    companion object {
        fun of(
            providerId: String,
            attributes: Map<String, Any>,
        ): OAuthAttributes =
            when {
                OAuthProvider.KAKAO.isProviderOf(providerId) -> KakaoAuthAttributes.of(attributes)
                else -> throw UnsupportedOAuthProviderException()
            }
    }
}
