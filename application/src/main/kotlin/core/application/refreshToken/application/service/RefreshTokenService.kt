package core.application.refreshToken.application.service

import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.exception.TokenNotFoundException
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefreshTokenService(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val tokenResolver: JwtTokenResolver,
    private val tokenInjector: JwtTokenInjector,
    private val tokenProvider: JwtTokenProvider,
) {
    /**
     * HTTP Header에서 Refresh Token을 추출하여, 해당 토큰을 기반으로 Access Token을 재발급하고,
     * RTR(Refresh Token Rotation) 전략에 따라 Refresh Token도 재발급함.
     *
     * @throws TokenInvalidException
     * @throws TokenNotFoundException
     *
     * @author LeeHanEum
     * @author its-sky
     * @since 2025.07.17
     */
    @Transactional
    fun reissueBasedOnRefreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val token =
            tokenResolver.resolveRefreshTokenFromRequest(request)
                ?: throw TokenInvalidException()

        val refreshToken: RefreshToken = getByTokenString(token)
        tokenInjector.injectRefreshToken(rotate(refreshToken), response)
        return tokenProvider.generateAccessToken(refreshToken.memberId.toString())
    }

    private fun getByTokenString(token: String): RefreshToken {
        return refreshTokenPersistencePort.findByToken(token)
            ?: throw TokenNotFoundException()
    }

    private fun rotate(refreshToken: RefreshToken): RefreshToken {
        val reissuedToken = tokenProvider.generateRefreshToken(refreshToken.memberId.toString())
        refreshToken.rotate(reissuedToken)
        return refreshTokenPersistencePort.save(refreshToken)
    }
}
