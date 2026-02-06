package core.application.member.presentation.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.net.URI

@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
) {
    companion object {
        private const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
        private const val APPLE_REDIRECT_URL = "redirect:/oauth2/authorization/apple"
        private const val REQUEST_DOMAIN = "REQUEST_DOMAIN"
        private const val ORIGIN = "Origin"
        private const val REFERER = "Referer"
    }

    private val logger = KotlinLogging.logger { MemberLoginController::class.java }

    @GetMapping("/login/kakao")
    @Operation(
        summary = "Kakao OAuth2 Login Redirect",
        description =
            "Initiates Kakao OAuth2 authorization flow." +
                "Sets REQUEST_DOMAIN cookie and redirects to Kakao authorization page.",
    )
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        setCookie(request, response)
        return KAKAO_REDIRECT_URL
    }

    @Operation(
        summary = "Apple OAuth2 Login Redirect",
        description =
            "Initiates Apple OAuth2 authorization flow." +
                "Sets REQUEST_DOMAIN cookie and redirects to Apple authorization page.",
    )
    @GetMapping("/login/apple")
    fun appleLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        setCookie(request, response)
        return APPLE_REDIRECT_URL
    }

    data class AppleLoginRequest(
        val authorizationCode: String,
    )

    private fun setCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
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
    }
}
