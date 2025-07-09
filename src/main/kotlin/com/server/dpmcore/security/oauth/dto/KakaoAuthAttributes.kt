package com.server.dpmcore.security.oauth.dto

import com.server.dpmcore.member.memberOAuth.domain.OAuthProvider

/**
 * 카카오 OAuth 인증 정보를 나타내는 데이터 클래스입니다.
 *
 * @property externalId 카카오 사용자 ID
 * @property email 카카오 계정 이메일
 * @property provider OAuth 제공자 (현재는 KAKAO만 지원)
 */
data class KakaoAuthAttributes(
    @JvmField val externalId: String,
    @JvmField val email: String,
    @JvmField val provider: OAuthProvider
) : OAuthAttributes {

    companion object {
        fun of(attributes: Map<String, Any>): KakaoAuthAttributes {
            val externalId = attributes["id"] as Long

            val kakaoAccount = attributes["kakao_account"] as Map<*, *>

            val email = kakaoAccount["email"] as String

            return KakaoAuthAttributes(
                externalId = externalId.toString(),
                email = email,
                provider = OAuthProvider.KAKAO
            )
        }
    }

    override fun getExternalId(): String {
        return this.externalId
    }

    override fun getProvider(): OAuthProvider {
        return this.provider
    }

    override fun getEmail(): String {
        return this.email
    }

}
