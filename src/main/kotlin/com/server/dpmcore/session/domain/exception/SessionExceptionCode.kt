package com.server.dpmcore.session.domain.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class SessionExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "S404", "세션을 찾을 수 없습니다"),
    SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "S409", "이미 존재하는 세션입니다"),
    INVALID_SESSION_STATE(HttpStatus.BAD_REQUEST, "S400", "유효하지 않은 세션 상태입니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
