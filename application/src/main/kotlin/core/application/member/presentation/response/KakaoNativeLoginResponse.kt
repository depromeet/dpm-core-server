package core.application.member.presentation.response

import core.domain.member.enums.MemberStatus

data class KakaoNativeLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val memberId: Long,
    val memberStatus: MemberStatus?,
) {
    companion object {
        fun loginSuccess(
            accessToken: String,
            refreshToken: String,
            memberId: Long,
            memberStatus: MemberStatus?,
        ): KakaoNativeLoginResponse =
            KakaoNativeLoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                memberId = memberId,
                memberStatus = memberStatus,
            )
    }
}
