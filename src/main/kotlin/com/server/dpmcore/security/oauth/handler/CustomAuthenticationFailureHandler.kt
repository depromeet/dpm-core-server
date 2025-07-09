package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.security.oauth.exception.OAuthExceptionCode
import com.server.dpmcore.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationFailureHandler(
    private val securityProperties: SecurityProperties
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?
    ) {
        response?.sendRedirect(
            securityProperties.loginUrl + "?error=true&exception="
                    + OAuthExceptionCode.AUTHENTICATION_FAILED
        )
    }
}
