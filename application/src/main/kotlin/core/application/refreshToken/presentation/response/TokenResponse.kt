package core.application.refreshToken.presentation.response

import core.application.refreshToken.application.dto.ReissueResult
import core.application.security.properties.TokenProperties
import io.swagger.v3.oas.annotations.media.Schema

data class TokenResponse(
    @field:Schema(
        description = "액세스 토큰",
        example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MjAxOTcyNzc",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val token: String,
    @field:Schema(
        description = "액세스 토큰 만료 시간(초)",
        example = "7200",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val expirationTime: Long,
    @field:Schema(
        description = "회전된 리프레시 토큰. 클라이언트는 이 값으로 저장소를 갱신해야 합니다.",
        example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MjAxOTcyNzc",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refreshToken: String,
    @field:Schema(
        description = "리프레시 토큰 만료 시간(초)",
        example = "2592000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refreshTokenExpirationTime: Long,
) {
    companion object {
        fun of(
            result: ReissueResult,
            tokenProperties: TokenProperties,
        ): TokenResponse =
            TokenResponse(
                token = result.accessToken,
                expirationTime = tokenProperties.expirationTime.accessToken,
                refreshToken = result.refreshToken,
                refreshTokenExpirationTime = tokenProperties.expirationTime.refreshToken,
            )
    }
}
