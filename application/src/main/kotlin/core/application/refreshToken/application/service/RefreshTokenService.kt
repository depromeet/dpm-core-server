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
        val stored = resolveStoredToken(request)
        val now = Instant.now()

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

    /**
     * Bearer 우선, 쿠키 폴백. 후보 중 저장소에 실제로 존재하는 첫 번째를 채택한다.
     *
     * "첫 번째 유효한 JWT" 를 고르면 안 된다. 클라이언트는 일반 API 인증용으로 Authorization 에
     * 액세스 토큰을 싣는데, 액세스와 리프레시는 서명 키도 클레임도 같아 validateToken 만으로는
     * 구분되지 않는다. 그래서 Bearer 의 액세스 토큰이 리프레시 토큰으로 채택되고, 쿠키에 멀쩡한
     * 리프레시 토큰이 함께 실려 있어도 TOKEN_NOT_FOUND 로 떨어진다.
     * 저장소에 있는지를 판정 기준으로 삼으면 액세스 토큰은 자연히 건너뛴다.
     */
    private fun resolveStoredToken(request: HttpServletRequest): RefreshToken {
        val candidates =
            tokenResolver
                .resolveRefreshTokenCandidatesFromRequest(request)
                .filter { tokenProvider.validateToken(it) }

        if (candidates.isEmpty()) {
            logDiagnostics(request)
            throw TokenInvalidException()
        }

        return candidates.firstNotNullOfOrNull {
            refreshTokenPersistencePort.findByTokenHash(TokenHasher.sha256Hex(it))
        } ?: run {
            logDiagnostics(request)
            throw TokenNotFoundException()
        }
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
