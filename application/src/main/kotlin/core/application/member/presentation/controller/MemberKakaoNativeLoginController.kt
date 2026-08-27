package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.auth.KakaoNativeLoginService
import core.application.member.presentation.response.KakaoNativeLoginResponse
import core.application.security.oauth.exception.OAuthAuthenticationFailedException
import core.application.security.oauth.exception.OAuthExceptionCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member-Login", description = "Member Login API")
@RestController
@RequestMapping(value = ["/v1/auth/kakao", "/api/v1/auth/kakao"])
class MemberKakaoNativeLoginController(
    private val kakaoNativeLoginService: KakaoNativeLoginService,
) {
    @PostMapping("/native")
    @Operation(
        summary = "Kakao Native Login",
        description = "Authenticates a user with a Kakao native access token and returns backend-issued JWT tokens.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Native login processed successfully"),
            ApiResponse(responseCode = "400", description = "Authorization header is missing or malformed"),
            ApiResponse(responseCode = "401", description = "Invalid Kakao access token"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun login(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false)
        authorizationHeader: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): CustomResponse<KakaoNativeLoginResponse> {
        val kakaoAccessToken = extractBearerToken(authorizationHeader)
        return CustomResponse.ok(kakaoNativeLoginService.login(kakaoAccessToken, request, response))
    }

    private fun extractBearerToken(authorizationHeader: String?): String {
        if (authorizationHeader.isNullOrBlank()) {
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_REQUIRED)
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_REQUIRED)
        }

        return authorizationHeader
            .removePrefix(BEARER_PREFIX)
            .trim()
            .takeIf { it.isNotBlank() }
            ?: throw OAuthAuthenticationFailedException(OAuthExceptionCode.KAKAO_ACCESS_TOKEN_REQUIRED)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
