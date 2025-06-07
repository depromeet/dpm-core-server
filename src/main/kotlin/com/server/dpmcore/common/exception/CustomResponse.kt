package com.server.dpmcore.common.exception

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.http.HttpStatus

@JsonPropertyOrder("status", "message", "code", "data")
data class CustomResponse<T>(
    val status: HttpStatus,
    val message: String,
    val code: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T? = null
) {
    companion object {
        fun <T> ok(): CustomResponse<T> = ok(null)

        fun <T> ok(data: T?): CustomResponse<T> =
            CustomResponse(
                GlobalExceptionCode.SUCCESS.status,
                GlobalExceptionCode.SUCCESS.message,
                GlobalExceptionCode.SUCCESS.code,
                data
            )

        fun error(exceptionCode: ExceptionCode): CustomResponse<Void> =
            error(exceptionCode, exceptionCode.getMessage())

        fun error(exceptionCode: ExceptionCode, message: String): CustomResponse<Void> =
            CustomResponse(
                exceptionCode.getStatus(),
                message,
                exceptionCode.getCode()
            )
    }

}