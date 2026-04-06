package core.application.security.oauth.handler

import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class CustomAuthenticationFailureHandler(
    private val securityProperties: SecurityProperties,
) : AuthenticationFailureHandler {
    /**
     * 소셜 로그인 인증 실패 시 호출되며, 프론트엔드가 오류를 해석할 수 있도록 단일 리다이렉트 URL로 이동시킵니다.
     *
     * @author LeeHanEum
     * @since 2025.07.12
     */
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?,
    ) {
        response?.sendRedirect(
            buildErrorRedirectUrl(OAuthExceptionCode.AUTHENTICATION_FAILED.name),
        )
    }

    private fun buildErrorRedirectUrl(error: String): String =
        securityProperties.redirect.redirectUrl +
            "?error=true&exception=" +
            URLEncoder.encode(error, StandardCharsets.UTF_8)
}
