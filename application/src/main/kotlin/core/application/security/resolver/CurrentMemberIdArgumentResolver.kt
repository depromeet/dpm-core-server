package core.application.security.resolver

import core.application.common.exception.UnauthorizedException
import core.application.security.annotation.CurrentMemberId
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentMemberIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMemberId::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null ||
            authentication is AnonymousAuthenticationToken ||
            authentication.name == "anonymousUser"
        ) {
            throw UnauthorizedException()
        }

        return SecurityContextHolder
            .getContext()
            .authentication.name
            .toLong()
    }
}
