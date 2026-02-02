package core.application.member.presentation.controller

import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import core.application.security.properties.SecurityProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberAppleLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
) {
    @PostMapping("/login/auth/apple")
    @Operation(
        summary = "Apple OAuth2 Login V1",
        description = "Login with Apple authorization code to receive JWT tokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun appleLoginV1(
        @RequestBody body: MemberLoginController.AppleLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = appleAuthService.login(body.authorizationCode)
        addTokenCookies(response, tokens)
        return tokens
    }
    private fun addTokenCookies(
        response: HttpServletResponse,
        tokens: AuthTokenResponse,
    ) {
        val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24) // 1 day
        val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30) // 30 days

        response.addCookie(accessTokenCookie)
        response.addCookie(refreshTokenCookie)
    }
    private fun createCookie(
        name: String,
        value: String,
        maxAgeSeconds: Int,
    ): Cookie {
        return Cookie(name, value).apply {
            path = "/"
            domain =
                if (securityProperties.cookie.domain != "localhost") {
                    securityProperties.cookie.domain
                } else {
                    null
                }
            maxAge = maxAgeSeconds
            isHttpOnly = true
            secure = securityProperties.cookie.secure // Use config value (true for dev/prod, false for local)
            setAttribute("SameSite", "None")
        }
    }
}
