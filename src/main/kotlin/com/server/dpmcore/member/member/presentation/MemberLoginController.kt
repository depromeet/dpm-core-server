package com.server.dpmcore.member.member.presentation

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MemberLoginController {
    companion object {
        const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
    }

    @GetMapping("/login/kakao")
    fun login(): String = KAKAO_REDIRECT_URL
}
