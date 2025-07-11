package com.server.dpmcore.security.presentation

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class RedirectController {

    @GetMapping("/login")
    fun login(): String = "login"

    @RequestMapping("/")
    fun catchAll(): String = "index"
}
