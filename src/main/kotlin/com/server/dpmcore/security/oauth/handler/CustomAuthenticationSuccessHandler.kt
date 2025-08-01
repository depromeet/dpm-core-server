package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.member.member.domain.port.inbound.HandleMemberLoginUseCase
import com.server.dpmcore.security.oauth.domain.CustomOAuth2User
import com.server.dpmcore.security.oauth.dto.LoginResult
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URI

@Component
class CustomAuthenticationSuccessHandler(
    private val tokenInjector: JwtTokenInjector,
    private val handleMemberLoginUseCase: HandleMemberLoginUseCase,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        resolveLoginResultFromAuthentication(
            getRequestDomain(request),
            authentication,
        ).let { loginResult ->
            println(getRequestDomain(request))
            loginResult.refreshToken?.let {
                tokenInjector.injectRefreshToken(it, response)
            }
            response.sendRedirect(loginResult.redirectUrl)
        }
    }

    private fun getRequestDomain(request: HttpServletRequest): String {
        val raw =
            request.getHeader("Origin")
                ?: request.getHeader("Referer")
                ?: request.requestURL.toString()

        return try {
            URI(raw).host
        } catch (e: Exception) {
            null
        } ?: throw IllegalArgumentException("Cannot extract host from request source: $raw")
    }

    private fun resolveLoginResultFromAuthentication(
        requestDomain: String,
        authentication: Authentication,
    ): LoginResult {
        val oAuth2User = authentication.principal as CustomOAuth2User
        return handleMemberLoginUseCase.handleLoginSuccess(requestDomain, oAuth2User.authAttributes)
    }
}
