package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.security.oauth.exception.OAuthExceptionCode
import com.server.dpmcore.security.redirect.model.LoginIntent
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.strategy.CompositeRedirectStrategy
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
