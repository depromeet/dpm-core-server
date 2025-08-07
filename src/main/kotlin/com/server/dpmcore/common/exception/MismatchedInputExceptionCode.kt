package com.server.dpmcore.common.exception

import org.springframework.http.HttpStatus

class MismatchedInputExceptionCode(
    @JvmField val status: HttpStatus = HttpStatus.BAD_REQUEST,
    @JvmField val code: String = "FIELD-400-01",
    fieldName: String,
) : ExceptionCode {
    val errorMessage = "필드 '$fieldName'의 값이 누락되었거나 타입이 맞지 않습니다."

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = errorMessage
}
