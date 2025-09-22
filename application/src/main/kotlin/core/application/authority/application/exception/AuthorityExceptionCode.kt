package core.application.authority.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class AuthorityExceptionCode(
    @JvmField
    val status: HttpStatus,
    @JvmField
    val code: String,
    @JvmField
    val message: String,
) : ExceptionCode {
    AUTHORITY_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTHORITY-404-01", "권한을 찾을 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
