package core.application.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.security.oauth.exception.JwtExceptionCode
import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val oAuthCallbackRedirectService: OAuthCallbackRedirectService,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        if (request.requestURI.startsWith(OAUTH_CALLBACK_PATH_PREFIX)) {
            val redirectUri =
                oAuthCallbackRedirectService.buildFailureRedirectUri(
                    request,
                    JwtExceptionCode.TOKEN_INVALID.getCode(),
                )
            oAuthCallbackRedirectService.clearClientRedirectUri(response)
            response.sendRedirect(redirectUri)
            return
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            CustomResponse.error(
                JwtExceptionCode.TOKEN_INVALID,
                authException.message ?: "인증이 필요합니다",
            )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    companion object {
        private const val OAUTH_CALLBACK_PATH_PREFIX = "/login/oauth2/code/"
    }
}
