package com.server.dpmcore.member.member.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberLoginController {
    companion object {
        const val KAKAO_AUTHORIZATION_PATH = "/oauth2/authorization/kakao"
    }

    @GetMapping("/login/kakao")
    fun login(): OAuthAuthorizationPathResponse = OAuthAuthorizationPathResponse(KAKAO_AUTHORIZATION_PATH)

    data class OAuthAuthorizationPathResponse(
        val authorizationPath: String,
    )
}
