package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.security.oauth.domain.CustomOAuth2User
import com.server.dpmcore.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationSuccessHandler(
    private val securityProperties: SecurityProperties,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val result = resolveLoginResultFromAuthentication(authentication)
        // TODO: 쿠키에 토큰을 주입 로직 추가
        response.sendRedirect(securityProperties.redirectUrl)
    }

    private fun resolveLoginResultFromAuthentication(authentication: Authentication) {
        val oAuth2User = authentication.principal as CustomOAuth2User
        // TODO: 최초 로그인 여부에 따른 로직 유저 정보 저장 및 토큰 발급 로직 추가
    }
}
