package com.server.dpmcore.security.oauth.dto

import com.server.dpmcore.refreshToken.domain.model.RefreshToken

data class LoginResult(
    val refreshToken: RefreshToken,
    val redirectUrl: String
)
