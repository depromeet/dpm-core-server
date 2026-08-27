package core.application.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomLogoutSuccessHandler(
    private val tokenInjector: JwtTokenInjector,
    private val tokenProvider: JwtTokenProvider,
    private val tokenResolver: JwtTokenResolver,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val objectMapper: ObjectMapper,
) : LogoutSuccessHandler {
    private val logger = KotlinLogging.logger { }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
        private const val CHARACTER_ENCODING = "UTF-8"
    }

    /**
     * 로그아웃 성공 시 호출된다. 클라이언트의 Refresh Token 쿠키를 무효화하고,
     * 요청이 제시한 리프레시 토큰에 해당하는 **현재 기기의 세션만** 서버에서 삭제한다.
     *
     * 리프레시 토큰을 식별할 수 없으면 기존 동작(전 기기 로그아웃)으로 폴백한다.
     * 로그아웃이 아무 일도 하지 않는 상태가 되는 편이 더 위험하기 때문이다.
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        tokenInjector.invalidateRefreshToken(response)

        if (destroyCurrentDeviceSession(request)) {
            finish(response)
            return
        }

        destroyAllDeviceSessions(request)
        finish(response)
    }

    /** @return 현재 기기 세션을 특정해 삭제했으면 true. */
    private fun destroyCurrentDeviceSession(request: HttpServletRequest): Boolean {
        val presented =
            tokenResolver
                .resolveRefreshTokenCandidatesFromRequest(request)
                .firstOrNull { tokenProvider.validateToken(it) }
                ?: return false

        val tokenHash = TokenHasher.sha256Hex(presented)
        if (refreshTokenPersistencePort.findByTokenHash(tokenHash) == null) {
            return false
        }

        logger.info { "Invalidating current device session during logout" }
        refreshTokenInvalidator.destroyByTokenHash(tokenHash)
        return true
    }

    private fun destroyAllDeviceSessions(request: HttpServletRequest) {
        val token =
            request
                .getHeader(HEADER_AUTHORIZATION)
                ?.removePrefix(TOKEN_PREFIX)
                ?.trim()

        if (token.isNullOrBlank() || !tokenProvider.validateToken(token)) {
            return
        }

        val memberId = MemberId(tokenProvider.getMemberId(token))
        logger.warn {
            "Device-scoped logout not possible, falling back to all devices for memberId=${memberId.value}"
        }
        refreshTokenInvalidator.destroyRefreshToken(memberId)
    }

    private fun finish(response: HttpServletResponse) {
        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_OK
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = CHARACTER_ENCODING
        response.writer.write(objectMapper.writeValueAsString(CustomResponse.ok<Unit>()))
    }
}
