package com.server.dpmcore.member.member.presentation

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.net.URI

@Controller
class MemberLoginController {
    companion object {
        const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
        const val REQUEST_DOMAIN = "REQUEST_DOMAIN"
    }

    @GetMapping("/login/kakao")
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        try {
            val requestDomain =
                URI(request.getHeader("Origin") ?: request.getHeader("Referer") ?: request.requestURL.toString()).host

            Cookie(REQUEST_DOMAIN, requestDomain).apply {
                path = "/"
                maxAge = 60
                isHttpOnly = true
                secure = true
                response.addCookie(this)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot extract host from request source")
        }
        return KAKAO_REDIRECT_URL
    }
}
