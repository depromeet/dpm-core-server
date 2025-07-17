package com.server.dpmcore.common.exception

import org.springframework.http.HttpStatus

enum class GlobalExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SUCCESS(HttpStatus.OK, "G000", "요청에 성공했습니다"),
    CREATED(HttpStatus.CREATED, "G201", "요청에 성공하여 리소스가 생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "G204", "요청에 성공했지만 반환할 데이터가 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G400", "올바른 입력 형식이 아닙니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G500", "예상치 못한 서버 에러가 발생했습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
