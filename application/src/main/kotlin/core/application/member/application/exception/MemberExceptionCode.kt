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
    APPLE_LOGIN_MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "MEMBER-403-02", "Apple 로그인 회원만 사용할 수 있습니다"),
    MEMBER_SELF_DELETION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "MEMBER-403-03", "관리자는 본인 계정을 삭제할 수 없습니다"),
    INVALID_MEMBER_PART(HttpStatus.BAD_REQUEST, "MEMBER-400-05", "유효하지 않은 멤버 파트입니다"),
    MEMBER_DELETED(HttpStatus.UNAUTHORIZED, "MEMBER-401-02", "탈퇴한 회원입니다"),
    INVALID_MEMBER_MANAGEMENT_UPDATE(HttpStatus.BAD_REQUEST, "MEMBER-400-06", "멤버 수정 요청이 올바르지 않습니다"),
    MEMBER_MANAGEMENT_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER-400-07", "현재 기수의 승인된 멤버만 수정할 수 있습니다"),
    INVALID_MEMBER_MANAGEMENT_TEAM(HttpStatus.BAD_REQUEST, "MEMBER-400-08", "현재 기수에 속한 팀을 선택해주세요"),
    INVALID_MEMBER_APPROVAL(HttpStatus.BAD_REQUEST, "MEMBER-400-09", "가입 승인 요청이 올바르지 않습니다"),
    MEMBER_APPROVAL_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER-400-10", "현재 기수 또는 기수 없는 가입 대기자만 승인할 수 있습니다"),
    MEMBER_REJECTION_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER-400-11", "현재 기수 또는 기수 없는 가입 대기자만 반려할 수 있습니다"),
    MEMBER_REAPPLICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER-400-12", "반려된 회원만 가입을 재신청할 수 있습니다"),
    MEMBER_ADMISSION_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER-400-13", "가입 반려와 재신청은 전용 API를 사용해주세요"),
    INVALID_MEMBER_MERGE(HttpStatus.BAD_REQUEST, "MEMBER-400-21", "현재 기수 또는 기수 없는 서로 다른 소셜 가입 대기 계정만 통합할 수 있습니다"),
    MEMBER_OAUTH_CONFLICT(HttpStatus.CONFLICT, "MEMBER-409-21", "로그인 수단이 충돌하거나 소유 계정이 변경되었습니다. 다시 로그인해주세요"),
    MEMBER_MANAGEMENT_NOT_IMPLEMENTED(
        HttpStatus.NOT_IMPLEMENTED,
        "MEMBER-501-01",
        "명세만 제공하는 API입니다. 기능 구현 후 사용할 수 있습니다",
    ),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
