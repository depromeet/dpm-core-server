package core.application.security.oauth.token

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.common.logging.MdcLoggingFilter
import core.application.security.oauth.exception.JwtExceptionCode
import core.domain.member.port.outbound.MemberPersistencePort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenResolver: JwtTokenResolver,
    private val objectMapper: ObjectMapper,
    private val members: MemberPersistencePort,
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

        // 인증이 필요 없는 문서 경로라 토큰을 읽지 않는다.
        private val EXCLUDED_PATH_PREFIXES =
            listOf(
                "/swagger-ui",
                "/v3/api-docs",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorizationHeader = request.getHeader(HEADER_AUTHORIZATION)
        val bearerToken = getAccessToken(authorizationHeader)
        val tokenCandidates =
            buildList {
                bearerToken?.let(::add)
                getAccessTokenFromCookie(request)?.let(::add)
                jwtTokenResolver.resolveRefreshTokenCandidatesFromRequest(request)
                    .forEach(::add)
            }.distinct()

        val authenticatedToken =
            tokenCandidates.firstOrNull { jwtTokenProvider.validateToken(it) }

        if (authenticatedToken != null) {
            val memberId = runCatching { jwtTokenProvider.getMemberId(authenticatedToken) }.getOrNull()
            if (memberId == null || !members.isLoginAvailable(memberId)) {
                SecurityContextHolder.clearContext()
                writeUnauthorized(response, JwtExceptionCode.TOKEN_INVALID)
                return
            }
            val authentication = jwtTokenProvider.getAuthentication(authenticatedToken)
            SecurityContextHolder.getContext().authentication = authentication
            // 알림은 AsyncAppender 의 워커 스레드에서 조립되어 SecurityContext 를 볼 수 없다.
            // MDC 는 로그 이벤트에 스냅샷으로 실려 넘어가므로, 인증 직후 여기서 넣어둔다. 정리는 MdcLoggingFilter 가 한다.
            MDC.put(MdcLoggingFilter.MEMBER_ID, authentication.name)
        } else if (authorizationHeader != null &&
            authorizationHeader.isNotEmpty() &&
            !authorizationHeader.startsWith(TOKEN_PREFIX)
        ) {
            writeUnauthorized(response, JwtExceptionCode.AUTHORIZATION_HEADER_INVALID)
            return
        } else if (bearerToken != null) {
            writeUnauthorized(response, JwtExceptionCode.TOKEN_INVALID)
            return
        }
        // 쿠키 토큰만 유효하지 않은 경우(만료, 다른 환경에서 발급 등)는 무시하고 비로그인 요청으로 진행한다.
        // 쿠키는 Domain=depromeet.com 으로 prod·dev 간에 공유되므로, 여기서 막으면 공개 경로까지 실패한다.
        // 인증이 필요한 경로는 이후 AuthenticationEntryPoint 가 401 로 응답한다.

        filterChain.doFilter(request, response)
    }

    /** 필터에서 던진 예외는 GlobalExceptionHandler 가 잡지 못해 500 이 되므로 직접 401 로 응답한다. */
    private fun writeUnauthorized(
        response: HttpServletResponse,
        exceptionCode: JwtExceptionCode,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(CustomResponse.error(exceptionCode)))
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI in EXCLUDED_PATHS ||
            EXCLUDED_PATH_PREFIXES.any { request.requestURI.startsWith(it) }

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
