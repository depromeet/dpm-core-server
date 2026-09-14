package core.application.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청 정보를 MDC 에 담아 로그와 Discord 알림에서 맥락을 볼 수 있게 한다.
 *
 * 이게 없으면 알림에 예외 이름만 남아 어떤 API 의 어떤 요청이 터졌는지 알 수 없다.
 * 스레드 풀은 재사용되므로 요청이 끝나면 반드시 지운다.
 */
class MdcLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH))
            MDC.put(HTTP_METHOD, request.method)
            MDC.put(REQUEST_URI, requestUriWithQuery(request))
            MDC.put(CLIENT_IP, request.remoteAddr.orEmpty())

            filterChain.doFilter(request, response)
        } finally {
            KEYS.forEach(MDC::remove)
        }
    }

    private fun requestUriWithQuery(request: HttpServletRequest): String =
        request.queryString
            ?.let { "${request.requestURI}?$it" }
            ?: request.requestURI

    companion object {
        const val REQUEST_ID = "requestId"
        const val HTTP_METHOD = "httpMethod"
        const val REQUEST_URI = "requestUri"
        const val CLIENT_IP = "clientIp"

        /**
         * 이 필터는 인증보다 먼저 돌기 때문에 여기서는 채울 수 없다.
         * JwtAuthenticationFilter 가 인증에 성공한 뒤에 넣고, 정리는 바깥에 있는 이 필터가 맡는다.
         */
        const val MEMBER_ID = "memberId"

        private const val REQUEST_ID_LENGTH = 8
        private val KEYS = listOf(REQUEST_ID, HTTP_METHOD, REQUEST_URI, CLIENT_IP, MEMBER_ID)
    }
}
