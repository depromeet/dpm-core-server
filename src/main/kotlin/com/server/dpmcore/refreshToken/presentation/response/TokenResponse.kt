package com.server.dpmcore.refreshToken.presentation.response

import com.server.dpmcore.security.properties.TokenProperties
import io.swagger.v3.oas.annotations.media.Schema

data class TokenResponse(
    @field:Schema(
        description = "액세스 토큰",
        example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MjAxOTcyNzc",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val token: String,
    @field:Schema(
        description = "토큰 만료 시간(초)",
        example = "7200",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val expirationTime: Long,
) {
    companion object {
        fun of(
            token: String,
            tokenProperties: TokenProperties,
        ): TokenResponse {
            return TokenResponse(
                token = token,
                expirationTime = tokenProperties.expirationTime.accessToken,
            )
        }
    }
}
