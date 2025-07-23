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
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-1", "멤버를 찾을 수 없습니다"),
    INVALID_MEMBER_ID(HttpStatus.BAD_REQUEST, "MEMBER-400-1", "유효하지 않은 멤버 ID입니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
