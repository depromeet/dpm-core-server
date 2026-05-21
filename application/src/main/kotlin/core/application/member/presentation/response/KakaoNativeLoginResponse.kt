package core.application.member.presentation.response

import core.domain.member.enums.MemberStatus

data class KakaoNativeLoginResponse(
    val loginStatus: KakaoNativeLoginStatus,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val memberId: Long? = null,
    val memberStatus: MemberStatus? = null,
    val signupContext: SignupContext? = null,
) {
    data class SignupContext(
        val provider: String,
        val externalId: String,
        val email: String,
        val name: String,
    )

    companion object {
        fun loginSuccess(
            accessToken: String,
            refreshToken: String,
            memberId: Long,
            memberStatus: MemberStatus?,
        ): KakaoNativeLoginResponse =
            KakaoNativeLoginResponse(
                loginStatus = KakaoNativeLoginStatus.LOGIN_SUCCESS,
                accessToken = accessToken,
                refreshToken = refreshToken,
                memberId = memberId,
                memberStatus = memberStatus,
            )

        fun signupRequired(
            provider: String,
            externalId: String,
            email: String,
            name: String,
        ): KakaoNativeLoginResponse =
            KakaoNativeLoginResponse(
                loginStatus = KakaoNativeLoginStatus.SIGNUP_REQUIRED,
                signupContext =
                    SignupContext(
                        provider = provider,
                        externalId = externalId,
                        email = email,
                        name = name,
                    ),
            )
    }
}

enum class KakaoNativeLoginStatus {
    LOGIN_SUCCESS,
    SIGNUP_REQUIRED,
}
