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
    @Transactional
    fun reissueBasedOnRefreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val tokenCandidates =
            tokenResolver.resolveRefreshTokenCandidatesFromRequest(request)

        if (tokenCandidates.isEmpty()) {
            throw TokenInvalidException()
        }

        for (token in tokenCandidates.distinct()) {
            if (!tokenProvider.validateToken(token)) {
                continue
            }

            val refreshToken =
                refreshTokenPersistencePort.findByToken(token)
                    ?: continue

            tokenInjector.injectRefreshToken(rotate(refreshToken), response)
            return tokenProvider.generateAccessToken(refreshToken.memberId.toString())
        }

        throw TokenNotFoundException()
    }

    private fun rotate(refreshToken: RefreshToken): RefreshToken {
        val reissuedToken = tokenProvider.generateRefreshToken(refreshToken.memberId.toString())
        refreshToken.rotate(reissuedToken)
        return refreshTokenPersistencePort.save(refreshToken)
    }
}
