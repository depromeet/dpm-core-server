package com.server.dpmcore.security.oauth.dto

data class LoginResult(
    val refreshToken: String,
    val externalId: String,
    val isNewMember: Boolean,
)
