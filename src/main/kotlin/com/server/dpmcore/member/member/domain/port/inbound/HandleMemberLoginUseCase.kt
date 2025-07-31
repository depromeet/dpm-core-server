package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.security.oauth.dto.LoginResult
import com.server.dpmcore.security.oauth.dto.OAuthAttributes

interface HandleMemberLoginUseCase {
    fun handleLoginSuccess(
        requestDomain: String,
        authAttributes: OAuthAttributes,
    ): LoginResult
}
