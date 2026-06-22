package com.server.dpmcore.refreshToken.application

import com.server.dpmcore.refreshToken.application.exception.TokenInvalidException
import com.server.dpmcore.refreshToken.application.exception.TokenNotFoundException
import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import com.server.dpmcore.refreshToken.domain.port.outbound.RefreshTokenPersistencePort
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import com.server.dpmcore.security.oauth.token.JwtTokenProvider
import com.server.dpmcore.security.oauth.token.JwtTokenResolver
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
        val refreshTokenValue =
            tokenResolver.resolveRefreshTokenFromRequest(request)
                ?: throw TokenInvalidException()

        val refreshToken: RefreshToken = getByRefreshTokenString(refreshTokenValue)
        tokenInjector.injectRefreshToken(rotate(refreshToken), response)
        return tokenProvider.generateAccessToken(refreshToken.memberId.toString())
    }

    private fun getByRefreshTokenString(refreshToken: String): RefreshToken {
        return refreshTokenPersistencePort.findByToken(refreshToken)
            ?: throw TokenNotFoundException()
    }

    private fun rotate(refreshToken: RefreshToken): RefreshToken {
        val reissuedToken = tokenProvider.generateRefreshToken(refreshToken.memberId.toString())
        refreshToken.rotate(reissuedToken)
        return refreshTokenPersistencePort.save(refreshToken)
    }
}
