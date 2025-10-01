package core.application.security.oauth.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class OAuthExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    AUTHENTICATION_FAILED(HttpStatus.BAD_REQUEST, "OAUTH-400-1", "소셜 로그인에 실패했습니다"),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH-400-2", "지원하지 않는 OAuth 제공자입니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
