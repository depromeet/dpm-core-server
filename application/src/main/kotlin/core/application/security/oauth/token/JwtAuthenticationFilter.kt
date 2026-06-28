package core.application.security.oauth.token

import core.application.security.oauth.exception.InvalidAccessTokenException
import core.application.security.oauth.exception.JwtExceptionCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenResolver: JwtTokenResolver,
) : OncePerRequestFilter() {
    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val ACCESS_TOKEN_COOKIE = "accessToken"
        private const val TOKEN_PREFIX = "Bearer "
        private val EXCLUDED_PATHS =
            setOf(
                "/v1/auth/kakao/native",
                "/api/v1/auth/kakao/native",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorizationHeader = request.getHeader(HEADER_AUTHORIZATION)
        val tokenCandidates =
            buildList {
                getAccessToken(authorizationHeader)?.let(::add)
                getAccessTokenFromCookie(request)?.let(::add)
                jwtTokenResolver.resolveRefreshTokenCandidatesFromRequest(request)
                    .forEach(::add)
            }.distinct()

        val authenticatedToken =
            tokenCandidates.firstOrNull { jwtTokenProvider.validateToken(it) }

        if (authenticatedToken != null) {
            val authentication = jwtTokenProvider.getAuthentication(authenticatedToken)
            SecurityContextHolder.getContext().authentication = authentication
        } else if (authorizationHeader != null &&
            authorizationHeader.isNotEmpty() &&
            !authorizationHeader.startsWith(TOKEN_PREFIX)
        ) {
            throw InvalidAccessTokenException(JwtExceptionCode.AUTHORIZATION_HEADER_INVALID)
        } else if (tokenCandidates.isNotEmpty()) {
            throw InvalidAccessTokenException(JwtExceptionCode.TOKEN_INVALID)
        }

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI in EXCLUDED_PATHS

    private fun getAccessToken(authorizationHeader: String?): String? {
        if (authorizationHeader.isNullOrEmpty() || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return null
        }
        val token = authorizationHeader.substring(TOKEN_PREFIX.length)
        return token.ifEmpty { null }
    }

    private fun getAccessTokenFromCookie(request: HttpServletRequest): String? =
        request.cookies
            ?.lastOrNull { it.name == ACCESS_TOKEN_COOKIE }
            ?.value
            ?.takeIf { it.isNotBlank() }
}
