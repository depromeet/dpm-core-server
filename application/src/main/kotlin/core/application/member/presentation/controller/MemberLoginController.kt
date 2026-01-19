package core.application.member.presentation.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import core.application.security.properties.SecurityProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import java.net.URI

@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties
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
        description = "Initiates Kakao OAuth2 authorization flow. Sets REQUEST_DOMAIN cookie and redirects to Kakao authorization page.",
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
        description = "Initiates Apple OAuth2 authorization flow. Sets REQUEST_DOMAIN cookie and redirects to Apple authorization page.",
    )
    @GetMapping("/login/apple")
    fun appleLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        setCookie(request, response)
        return APPLE_REDIRECT_URL
    }
    /**
     * apple login to support third-party login
     * */
    @Operation(
        summary = "Apple OAuth2 Login V1",
        description = "Login with Apple authorization code to receive JWT tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping("/login/auth/apple")
    @ResponseBody
    fun appleLoginV1(
        @RequestBody body: AppleLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = appleAuthService.login(body.authorizationCode)
        addTokenCookies(response, tokens)
        return tokens
    }

    data class AppleLoginRequest(
        val authorizationCode: String,
    )

    private fun setCookie(request: HttpServletRequest, response: HttpServletResponse) {
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

    private fun addTokenCookies(response: HttpServletResponse, tokens: AuthTokenResponse) {
        val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24) // 1 day
        val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30) // 30 days

        response.addCookie(accessTokenCookie)
        response.addCookie(refreshTokenCookie)
    }

    private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
        return Cookie(name, value).apply {
            path = "/"
            domain = if (securityProperties.cookie.domain != "localhost") {
                securityProperties.cookie.domain
            } else {
                null
            }
            maxAge = maxAgeSeconds
            isHttpOnly = true
            secure = securityProperties.cookie.secure  // Use config value (true for dev/prod, false for local)
            setAttribute("SameSite", "None")
        }
    }
}
