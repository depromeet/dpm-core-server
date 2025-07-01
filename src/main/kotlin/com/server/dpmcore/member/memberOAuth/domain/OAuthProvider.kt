package com.server.dpmcore.member.memberOAuth.domain

/**
 * 서비스에서 지원하는 OAuth 제공자를 나타냅니다.
 * 현재는 카카오(Kakao)만 지원합니다.
 *
 * @property KAKAO 카카오 OAuth 제공자입니다.
 *
 * 추후 필요에 따라 다른 OAuth 제공자가 추가될 수 있습니다.
 */
enum class OAuthProvider {
    KAKAO,
    ;
}
