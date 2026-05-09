package core.application.member.presentation.controller

import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.application.service.auth.KakaoAuthService
import core.application.member.presentation.request.EmailPasswordLoginRequest
import core.application.security.oauth.service.OAuthAuthorizationUrl
import core.application.security.oauth.service.OAuthAuthorizationUrlService
import core.application.security.oauth.token.JwtTokenInjector
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberAppleLoginController(
    private val appleAuthService: AppleAuthService,
    private val kakaoAuthService: KakaoAuthService,
    private val oAuthAuthorizationUrlService: OAuthAuthorizationUrlService,
    private val emailPasswordAuthService: EmailPasswordAuthService,
    private val tokenInjector: JwtTokenInjector,
) {
    @GetMapping("/login/oauth/authorization/{provider}")
    @Operation(
        summary = "OAuth2 Authorization URL",
        description = "Returns a provider authorization URL for frontend-managed OAuth popup flows.",
    )
    fun authorizationUrl(
        @PathVariable provider: String,
        @RequestParam redirectUri: String,
        @RequestParam(required = false) state: String?,
    ): OAuthAuthorizationUrl =
        oAuthAuthorizationUrlService.buildAuthorizationUrl(
            provider = provider,
            redirectUri = redirectUri,
            state = state,
        )

    @PostMapping("/login/email")
    @Operation(
        summary = "Email Password Login",
        description =
            "Login with email and password to receive JWT tokens. " +
                "This is an isolated feature that can be easily deprecated.",
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
        val tokens =
            appleAuthService.login(
                authorizationCode = body.authorizationCode,
                fullName = body.fullName,
                familyName = body.familyName,
                givenName = body.givenName,
            )
        addTokenCookies(response, tokens)
        return tokens
    }

    @PostMapping("/login/auth/kakao")
    @Operation(
        summary = "Kakao OAuth2 Login V1",
        description = "Login with Kakao authorization code to receive JWT tokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun kakaoLoginV1(
        @RequestBody body: MemberLoginController.KakaoLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens =
            kakaoAuthService.login(
                authorizationCode = body.authorizationCode,
                redirectUri = body.redirectUri,
            )
        addTokenCookies(response, tokens)
        return tokens
    }

    private fun addTokenCookies(
        response: HttpServletResponse,
        tokens: AuthTokenResponse,
    ) {
        tokenInjector.injectAccessToken(tokens.accessToken, response)
        tokenInjector.injectRefreshToken(tokens.refreshToken, response)
    }
}
