package core.application.member.application.exception

import core.application.common.exception.ExceptionCode
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
    INVALID_MEMBER_ID(HttpStatus.BAD_REQUEST, "MEMBER-400-01", "유효하지 않은 멤버 ID입니다"),
    COHORT_MEMBERS_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-02", "기수에 속한 멤버를 찾을 수 없습니다"),
    MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER-400-2", "멤버 ID는 null일 수 없습니다"),
    MEMBER_TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-404-03", "멤버의 팀을 찾을 수 없습니다"),
    MEMBER_NAME_AUTHORITY_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER-400-3", "멤버 이름과 권한은 null일 수 없습니다"),
    MEMBER_STAUTS_ALREADY_UPDATED(HttpStatus.BAD_REQUEST, "MEMBER-400-4", "멤버 상태가 이미 업데이트 되었습니다"),
    INVALID_EMAIL_PASSWORD(HttpStatus.UNAUTHORIZED, "MEMBER-401-01", "이메일 또는 비밀번호가 올바르지 않습니다"),
    MEMBER_NOT_ALLOWED(HttpStatus.FORBIDDEN, "MEMBER-403-01", "로그인이 제한된 회원입니다"),
    MEMBER_DELETED(HttpStatus.UNAUTHORIZED, "MEMBER-401-02", "탈퇴한 회원입니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
