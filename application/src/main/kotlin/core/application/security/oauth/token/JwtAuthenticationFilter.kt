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
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorizationHeader = request.getHeader(HEADER_AUTHORIZATION)

        if (authorizationHeader != null && !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            throw InvalidAccessTokenException(JwtExceptionCode.AUTHORIZATION_HEADER_INVALID)
        }

        val token = getAccessToken(authorizationHeader)

        if (token != null) {
            if (!jwtTokenProvider.validateToken(token)) {
                throw InvalidAccessTokenException(JwtExceptionCode.TOKEN_INVALID)
            }
            val authentication = jwtTokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun getAccessToken(authorizationHeader: String?): String? =
        if (!authorizationHeader.isNullOrEmpty() && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            authorizationHeader.substring(TOKEN_PREFIX.length)
        } else {
            null
        }
}
