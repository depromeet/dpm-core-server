package com.server.dpmcore.member.member.application.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class MemberExceptionCode(
    @JvmField
    val status: HttpStatus,
    @JvmField
    val code: String,
    @JvmField
    val message: String,
) : ExceptionCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M404", "멤버를 찾을 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
