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
    KAKAO_ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "OAUTH-401-1", "유효하지 않은 Kakao 액세스 토큰입니다"),
    KAKAO_ACCESS_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "OAUTH-400-3", "Authorization 헤더에 Kakao 액세스 토큰이 필요합니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
