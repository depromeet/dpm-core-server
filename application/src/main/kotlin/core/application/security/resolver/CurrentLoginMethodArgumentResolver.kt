package core.application.security.resolver

import core.application.security.annotation.CurrentLoginMethod
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.enums.LoginMethod
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * JwtAuthenticationFilter 가 인증에 사용한 토큰은 Authentication.credentials 에 들어 있다.
 * 그 토큰의 로그인 수단 클레임을 읽는다. 인증은 이미 필터에서 끝났으므로 여기서는 예외를 던지지 않는다.
 */
@Component
class CurrentLoginMethodArgumentResolver(
    private val jwtTokenProvider: JwtTokenProvider,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentLoginMethod::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): LoginMethod? {
        val token = SecurityContextHolder.getContext().authentication?.credentials as? String ?: return null
        return runCatching { jwtTokenProvider.getLoginMethod(token) }.getOrNull()
    }
}
