package core.application.security.oauth.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class JwtExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "JWT-401-1", "유효하지 않은 액세스 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT-401-2", "액세스 토큰이 만료되었습니다"),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "JWT-401-3", "액세스 토큰 형식이 올바르지 않습니다"),
    AUTHORIZATION_HEADER_INVALID(HttpStatus.UNAUTHORIZED, "JWT-401-4", "Authorization 헤더 형식이 올바르지 않습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
