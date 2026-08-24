package core.application.refreshToken.application.service

import core.application.refreshToken.application.dto.ReissueResult
import core.application.refreshToken.application.exception.TokenExpiredException
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.exception.TokenNotFoundException
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefreshTokenService(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val refreshTokenIssueService: RefreshTokenIssueService,
    private val tokenResolver: JwtTokenResolver,
    private val tokenInjector: JwtTokenInjector,
    private val tokenProvider: JwtTokenProvider,
    private val deviceIdResolver: DeviceIdResolver,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun reissue(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ReissueResult {
        val presented = resolvePresentedToken(request)
        val now = Instant.now()

        val stored =
            refreshTokenPersistencePort.findByTokenHash(TokenHasher.sha256Hex(presented))
                ?: run {
                    logDiagnostics(request)
                    throw TokenNotFoundException()
                }

        if (stored.isExpired(now)) {
            throw TokenExpiredException()
        }

        if (stored.isRotated() && !stored.isWithinGrace(now, GRACE_SECONDS)) {
            // 재사용 감지. 1단계에서는 해당 체인만 폐기한다.
            // 회원 전체 토큰 폐기로의 강화는 이 로그가 충분히 낮아진 뒤 판단한다.
            logger.warn {
                "refresh token reuse detected: memberId=${stored.memberId.value} " +
                    "tokenId=${stored.tokenId} rotatedAt=${stored.rotatedAt}"
            }
            refreshTokenPersistencePort.deleteByTokenHash(stored.tokenHash)
            throw TokenInvalidException()
        }

        if (!stored.isRotated()) {
            refreshTokenPersistencePort.markRotated(stored.tokenHash, now)
        }

        val deviceId = stored.deviceId ?: deviceIdResolver.resolve(request, response)
        val issued = refreshTokenIssueService.issueForRotation(stored.memberId, deviceId)
        return respond(issued, response)
    }

    private fun respond(
        issued: RefreshToken,
        response: HttpServletResponse,
    ): ReissueResult {
        val accessToken = tokenProvider.generateAccessToken(issued.memberId.toString())
        tokenInjector.injectAccessToken(accessToken, response)
        tokenInjector.injectRefreshToken(issued, response)

        return ReissueResult(
            accessToken = accessToken,
            refreshToken = issued.requirePlainToken(),
            refreshTokenExpiresAt = issued.expiresAt,
        )
    }

    /** Bearer 우선, 쿠키 폴백. 유효한 JWT 후보 중 첫 번째를 쓴다. */
    private fun resolvePresentedToken(request: HttpServletRequest): String {
        val candidates = tokenResolver.resolveRefreshTokenCandidatesFromRequest(request)
        if (candidates.isEmpty()) {
            logDiagnostics(request)
            throw TokenInvalidException()
        }

        return candidates.firstOrNull { tokenProvider.validateToken(it) }
            ?: throw TokenInvalidException()
    }

    private fun logDiagnostics(request: HttpServletRequest) {
        logger.warn {
            "reissue failed: hasAuthorizationHeader=${request.getHeader(HttpHeaders.AUTHORIZATION) != null} " +
                "cookieCount=${request.cookies?.count { it.name == REFRESH_TOKEN_CAMEL_CASE } ?: 0} " +
                "hasDeviceCookie=${deviceIdResolver.peek(request) != null}"
        }
    }

    companion object {
        private const val GRACE_SECONDS = 60L
    }
}
