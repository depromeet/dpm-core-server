package com.server.dpmcore.gathering.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class GatheringExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SUCCESS(HttpStatus.OK, "GATHERING-200-01", "요청에 성공했습니다"),
    CREATE(HttpStatus.CREATED, "GATHERING-201-01", "리소스 생성에 성공했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "GATHERING-400-01", "올바른 입력 형식이 아닙니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GATHERING-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    GATHERING_NOT_FOUND(HttpStatus.BAD_REQUEST, "GATHERING-404-01", "존재하지 않는 회식입니다."),
    GATHERING_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING-404-02", "회식은 필수로 존재해야합니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
