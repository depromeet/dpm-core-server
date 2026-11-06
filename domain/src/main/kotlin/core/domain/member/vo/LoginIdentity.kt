package core.domain.member.vo

import core.domain.member.enums.LoginMethod

/**
 * 이번 세션에 로그인한 계정입니다. 로그인 시 토큰 클레임에 담기고, 토큰 재발급 시 그대로 이어집니다.
 *
 * @property method 로그인 수단
 * @property accountId 로그인한 계정의 식별자. KAKAO / APPLE 이면 member_oauth_id, EMAIL 이면 member_credential_id
 */
data class LoginIdentity(
    val method: LoginMethod,
    val accountId: Long,
    /** 인증된 요청에서만 채워지는 리프레시 세션 식별자. 로그인 수단 자체와는 별도로 검증한다. */
    val sessionTokenHash: String? = null,
)
