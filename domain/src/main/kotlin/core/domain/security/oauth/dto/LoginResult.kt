package core.domain.security.oauth.dto

import core.domain.refreshToken.aggregate.RefreshToken

data class LoginResult(
    val refreshToken: RefreshToken?,
    val redirectUrl: String,
)
