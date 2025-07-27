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
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-01", "멤버를 찾을 수 없습니다"),
    MEMBER_ID_CAN_NOT_BE_NULL(HttpStatus.BAD_REQUEST, "MEMBER-400-02", "멤버 ID는 null일 수 없습니다"),
    INVALID_MEMBER_ID(HttpStatus.BAD_REQUEST, "MEMBER-400-01", "유효하지 않은 멤버 ID입니다"),
    COHORT_MEMBERS_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-02", "기수에 속한 멤버를 찾을 수 없습니다"),
    MEMBER_TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-03", "멤버의 팀을 찾을 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
