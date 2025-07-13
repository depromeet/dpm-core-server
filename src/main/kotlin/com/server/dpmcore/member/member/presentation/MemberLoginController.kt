package com.server.dpmcore.member.member.presentation

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MemberLoginController {

    @GetMapping("/login")
    fun login(): String {
        return "redirect:/oauth2/authorization/kakao"
    }
}
