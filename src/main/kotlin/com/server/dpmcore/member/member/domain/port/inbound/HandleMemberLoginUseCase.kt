package com.server.dpmcore.member.member.domain.port.inbound

import com.server.dpmcore.security.oauth.dto.LoginResult
import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import jakarta.servlet.http.HttpServletRequest

interface HandleMemberLoginUseCase {
    /**
     * 멤버 로그인 시 리디렉션 URL을 결정하며, 신규 멤버인 경우 가입 정보를 저장함.
     *
     * @author leehaneum
     * @since 2025.08.15
     */
    fun handleLoginSuccess(
        request: HttpServletRequest,
        authAttributes: OAuthAttributes,
    ): LoginResult
}
