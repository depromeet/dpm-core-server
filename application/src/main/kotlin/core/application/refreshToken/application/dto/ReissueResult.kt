package core.application.refreshToken.application.dto

import java.time.Instant

data class ReissueResult(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)
