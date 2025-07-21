package com.server.dpmcore.cohort.domain.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class CohortExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    COHORT_NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "기수를 찾을 수 없습니다"),
    COHORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "C409", "이미 존재하는 기수입니다"),
    INVALID_COHORT_STATE(HttpStatus.BAD_REQUEST, "C400", "유효하지 않은 기수 상태입니다"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}
