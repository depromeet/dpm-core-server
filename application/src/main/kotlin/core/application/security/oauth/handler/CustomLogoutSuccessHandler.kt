package core.application.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
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

    /**
     * 로그아웃 성공 시 호출되는 메서드로, 클라이언트의 Refresh Token 쿠키를 무효화하고,
     * 서버에 저장된 Refresh Token을 삭제한 후, 성공 응답을 반환함.
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
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
