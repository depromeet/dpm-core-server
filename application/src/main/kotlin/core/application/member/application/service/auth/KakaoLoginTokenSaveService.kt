package core.application.member.application.service.auth

import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoLoginTokenSaveService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenInjector: JwtTokenInjector,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) {
    @Transactional
    fun save(
        accessToken: String,
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        if (!jwtTokenProvider.validateToken(accessToken) || !jwtTokenProvider.validateToken(refreshToken)) {
            throw TokenInvalidException()
        }

        val accessTokenMemberId = jwtTokenProvider.getMemberId(accessToken)
        val refreshTokenMemberId = jwtTokenProvider.getMemberId(refreshToken)

        if (accessTokenMemberId != refreshTokenMemberId) {
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
