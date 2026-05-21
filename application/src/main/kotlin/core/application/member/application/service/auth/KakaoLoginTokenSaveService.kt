package core.application.member.application.service.auth

import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoLoginTokenSaveService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenInjector: JwtTokenInjector,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun save(
        accessToken: String,
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        val accessTokenValid = jwtTokenProvider.validateToken(accessToken)
        val refreshTokenValid = jwtTokenProvider.validateToken(refreshToken)

        if (!accessTokenValid || !refreshTokenValid) {
            logger.warn {
                "Rejected Kakao token save request: invalid token pair " +
                    "(accessTokenValid=$accessTokenValid, refreshTokenValid=$refreshTokenValid)"
            }
            throw TokenInvalidException()
        }

        val accessTokenMemberId = jwtTokenProvider.getMemberId(accessToken)
        val refreshTokenMemberId = jwtTokenProvider.getMemberId(refreshToken)

        if (accessTokenMemberId != refreshTokenMemberId) {
            logger.warn {
                "Rejected Kakao token save request: token subjects differ " +
                    "(accessTokenMemberId=$accessTokenMemberId, refreshTokenMemberId=$refreshTokenMemberId)"
            }
            throw TokenInvalidException()
        }

        val refreshTokenEntity =
            refreshTokenPersistencePort.findByMemberId(refreshTokenMemberId)
                ?.apply { rotate(refreshToken) }
                ?: RefreshToken.create(MemberId(refreshTokenMemberId), refreshToken)

        refreshTokenPersistencePort.save(refreshTokenEntity)
        jwtTokenInjector.injectAccessToken(accessToken, response)
        jwtTokenInjector.injectRefreshToken(refreshToken, response)
    }
}
