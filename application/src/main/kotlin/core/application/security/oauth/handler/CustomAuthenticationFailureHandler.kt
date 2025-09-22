package core.application.security.oauth.handler

import core.application.security.redirect.model.LoginIntent
import core.application.security.redirect.model.RedirectContext
import core.application.security.redirect.strategy.CompositeRedirectStrategy
import core.application.security.oauth.exception.OAuthExceptionCode
import core.domain.authority.enums.AuthorityType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationFailureHandler(
    private val strategy: CompositeRedirectStrategy,
) : AuthenticationFailureHandler {
    /**
     * 소셜 로그인 인증 실패 시 호출되는 메서드로, 사용자를 오류 페이지로 리다이렉트 합니다.
     *
     * @author LeeHanEum
     * @since 2025.07.12
     */
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?,
    ) {
        val context =
            RedirectContext(
                authority = AuthorityType.GUEST,
                intent = LoginIntent.DIRECT,
                error = OAuthExceptionCode.AUTHENTICATION_FAILED.name,
            )

        response?.sendRedirect(
            strategy.resolve(context),
        )
    }
}
