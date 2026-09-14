package core.application.security.oauth.token

import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class JwtTokenResolver {
    /**
     * 재발급에 쓸 수 있는 리프레시 토큰 후보를 우선순위대로 돌려준다.
     *
     * Bearer 를 쿠키보다 먼저 두는 이유: 클라이언트가 헤더에 명시적으로 실은 값이 의도이고,
     * 쿠키는 과거 스코프의 잔재일 수 있다. 이전 구현은 쿠키를 먼저 채택해서,
     * 낡은 쿠키가 하나라도 남아 있으면 그 기기의 재발급이 영구히 실패했다.
     */
    fun resolveRefreshTokenCandidatesFromRequest(request: HttpServletRequest): List<String> =
        buildList {
            resolveFromBearerHeader(request.getHeader(HttpHeaders.AUTHORIZATION))
                ?.let(::add)

            request.cookies
                ?.filter { it.name == REFRESH_TOKEN_CAMEL_CASE }
                ?.mapNotNull { it.value.takeIf(String::isNotBlank) }
                ?.reversed()
                ?.let(::addAll)
        }.distinct()

    private fun resolveFromBearerHeader(authorizationHeader: String?): String? {
        if (authorizationHeader.isNullOrBlank()) {
            return null
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null
        }

        return authorizationHeader
            .removePrefix(BEARER_PREFIX)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
