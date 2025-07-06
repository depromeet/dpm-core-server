package com.server.dpmcore.common.exception

open class BusinessException(
    private val code: ExceptionCode,
) : RuntimeException(code.getMessage()) {
    fun getCode(): ExceptionCode = code
}
