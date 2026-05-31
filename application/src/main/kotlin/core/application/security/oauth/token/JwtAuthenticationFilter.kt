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
        val token = getAccessToken(authorizationHeader) ?: getAccessTokenFromCookie(request)

        if (authorizationHeader != null &&
            authorizationHeader.isNotEmpty() &&
            !authorizationHeader.startsWith(TOKEN_PREFIX) &&
            token == null
        ) {
            throw InvalidAccessTokenException(JwtExceptionCode.AUTHORIZATION_HEADER_INVALID)
        }

        if (token != null) {
            if (!jwtTokenProvider.validateToken(token)) {
                throw InvalidAccessTokenException(JwtExceptionCode.TOKEN_INVALID)
            }
            val authentication = jwtTokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
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
            ?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }
            ?.value
            ?.takeIf { it.isNotBlank() }
}
