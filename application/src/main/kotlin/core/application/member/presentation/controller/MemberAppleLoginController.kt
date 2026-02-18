package core.application.member.presentation.controller

import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.presentation.request.EmailPasswordLoginRequest
import core.application.security.properties.SecurityProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberAppleLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
    private val emailPasswordAuthService: EmailPasswordAuthService
) {

    @PostMapping("/login/email")
    @Operation(
        summary = "Email Password Login",
        description = "Login with email and password to receive JWT tokens. This is an isolated feature that can be easily deprecated.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid email or password"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun emailLogin(
        @RequestBody @Valid request: EmailPasswordLoginRequest,
        httpResponse: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = emailPasswordAuthService.login(request.email, request.password)
        addTokenCookies(httpResponse, tokens)
        return tokens
    }

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
        // 1 day
        val maxAgeSecondsForAccessToken = 60 * 60 * 24
        val accessTokenCookie =
            createAccessTokenCookie(
                value = tokens.accessToken,
                maxAgeSeconds = maxAgeSecondsForAccessToken,
            )

        // 30 days
        val maxAgeSecondsForRefreshToken = 60 * 60 * 24 * 30
        val refreshTokenCookie =
            createRefreshTokenCookie(
                value = tokens.refreshToken,
                maxAgeSeconds = maxAgeSecondsForRefreshToken,
            )

        response.addCookie(accessTokenCookie)
        response.addCookie(refreshTokenCookie)
    }

    private fun createAccessTokenCookie(
        value: String,
        maxAgeSeconds: Int,
    ): Cookie =
        Cookie("accessToken", value).apply {
            path = "/"
            domain = resolveDomain()
            maxAge = maxAgeSeconds
            isHttpOnly = true
            secure = securityProperties.cookie.secure
            setAttribute("SameSite", "Lax")
        }

    private fun createRefreshTokenCookie(
        value: String,
        maxAgeSeconds: Int,
    ): Cookie =
        Cookie("refreshToken", value).apply {
            path = "/"
            domain = resolveDomain()
            maxAge = maxAgeSeconds
            isHttpOnly = true
            secure = true
            setAttribute("SameSite", "None")
        }

    private fun resolveDomain(): String? =
        if (securityProperties.cookie.domain != "localhost") {
            securityProperties.cookie.domain
        } else {
            null
        }
}
