package com.server.dpmcore.member.member.presentation

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.net.URI

@Controller
class MemberLoginController {
    companion object {
        private const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
        private const val REQUEST_DOMAIN = "REQUEST_DOMAIN"
        private const val ORIGIN = "Origin"
        private const val REFERER = "Referer"
    }

    private val logger = KotlinLogging.logger { MemberLoginController::class.java }

    @GetMapping("/login/kakao")
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        try {
            val requestDomain =
                URI(request.getHeader(ORIGIN) ?: request.getHeader(REFERER) ?: request.requestURL.toString()).host

            Cookie(REQUEST_DOMAIN, requestDomain).apply {
                path = "/"
                maxAge = 60
                isHttpOnly = true
                secure = true
                response.addCookie(this)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to set REQUEST_DOMAIN cookie : ${e.message}" }
        }
        return KAKAO_REDIRECT_URL
    }
}
