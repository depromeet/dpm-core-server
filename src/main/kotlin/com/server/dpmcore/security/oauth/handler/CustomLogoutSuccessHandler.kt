package com.server.dpmcore.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.refreshToken.domain.port.inbound.RefreshTokenInvalidator
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import com.server.dpmcore.security.oauth.token.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomLogoutSuccessHandler(
    private val tokenInjector: JwtTokenInjector,
    private val tokenProvider: JwtTokenProvider,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
    private val objectMapper: ObjectMapper,
) : LogoutSuccessHandler {
    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
        private const val CHARACTER_ENCODING = "UTF-8"
    }

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        tokenInjector.invalidateCookie(REFRESH_TOKEN, response)

        val token =
            request
                .getHeader(HEADER_AUTHORIZATION)
                ?.removePrefix(TOKEN_PREFIX)
                ?.trim()

        if (!token.isNullOrBlank() && tokenProvider.validateToken(token)) {
            val memberId = MemberId(tokenProvider.getMemberId(token))
            refreshTokenInvalidator.destroyRefreshToken(memberId)
        }

        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_OK
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = CHARACTER_ENCODING
        response.writer.write(objectMapper.writeValueAsString(CustomResponse.ok<Unit>()))
    }
}
