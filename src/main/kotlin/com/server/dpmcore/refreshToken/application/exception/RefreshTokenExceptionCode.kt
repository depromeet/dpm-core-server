package com.server.dpmcore.refreshToken.application.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class RefreshTokenExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    TOKEN_INVALID(HttpStatus.BAD_REQUEST, "R403", "유효하지 않은 토큰입니다"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "R404", "토큰을 찾을 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
