package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.member.member.domain.port.inbound.HandleMemberLogoutUseCase
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomLogoutSuccessHandler(
    private val tokenInjector: JwtTokenInjector,
    private val handleMemberLogoutUseCase: HandleMemberLogoutUseCase,
) : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authentication: Authentication?,
    ) {
        // invalidate rt
        // destroy rt
    }
}
