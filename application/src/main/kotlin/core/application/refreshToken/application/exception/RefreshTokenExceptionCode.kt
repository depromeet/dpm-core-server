package core.application.refreshToken.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class RefreshTokenExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    // 이전에는 BAD_REQUEST(400) 였다. FE 의 refresh 플러그인이 401 로 분기하지 못하던 원인이라 교정했다.
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "R403", "유효하지 않은 토큰입니다"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "R404", "토큰을 찾을 수 없습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "R405", "리프레시 토큰이 만료되었습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
