package com.server.dpmcore.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.security.oauth.exception.OAuthExceptionCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationFailureHandler(
    private val objectMapper: ObjectMapper,
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?,
    ) {
        response ?: return

        response.status = OAuthExceptionCode.AUTHENTICATION_FAILED.getStatus().value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = CHARACTER_ENCODING
        response.writer.write(
            objectMapper.writeValueAsString(
                CustomResponse.error(
                    OAuthExceptionCode.AUTHENTICATION_FAILED,
                    exception?.message ?: OAuthExceptionCode.AUTHENTICATION_FAILED.getMessage(),
                ),
            ),
        )
    }

    companion object {
        private const val CHARACTER_ENCODING = "UTF-8"
    }
}
