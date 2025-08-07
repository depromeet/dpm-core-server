package com.server.dpmcore.common.exception

import org.springframework.http.HttpStatus

class JsonParseException(
    @JvmField val status: HttpStatus = HttpStatus.BAD_REQUEST,
    @JvmField val code: String = "JSON-400-01",
    @JvmField val message: String = "JSON 파싱 중 에러가 발생했습니다.",
) : ExceptionCode {
    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
