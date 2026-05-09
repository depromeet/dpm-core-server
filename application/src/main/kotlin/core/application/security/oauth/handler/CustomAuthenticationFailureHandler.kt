package core.application.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.security.oauth.exception.OAuthExceptionCode
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
    /**
     * 소셜 로그인 인증 실패 시 호출되며, 프론트엔드가 오류를 해석할 수 있도록 JSON 응답을 반환합니다.
     *
     * @author LeeHanEum
     * @since 2025.07.12
     */
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
