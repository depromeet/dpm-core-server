package core.application.security.presentation

import core.application.common.exception.CustomResponse
import core.application.security.oauth.token.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("local", "dev")
@RestController
@RequestMapping("/test/token")
@Tag(name = "Test-Token", description = "테스트용 JWT 토큰 생성 API (local, dev 환경 전용)")
class TestTokenController(
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @GetMapping("/generate")
    @Operation(
        summary = "JWT 토큰 생성",
        description = "테스트용 JWT Access Token과 Refresh Token을 생성합니다. memberId를 지정하지 않으면 기본값 11이 사용됩니다.",
    )
    fun generateToken(
        @Parameter(description = "멤버 ID", example = "1")
        @RequestParam memberId: Long,
    ): CustomResponse<TokenResponse> {
        val accessToken = jwtTokenProvider.generateAccessToken(memberId.toString())
        val refreshToken = jwtTokenProvider.generateRefreshToken(memberId.toString())

        return CustomResponse.ok(
            TokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                memberId = memberId,
            ),
        )
    }

    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val memberId: Long,
    )
}
