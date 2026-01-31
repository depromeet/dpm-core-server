package core.application.announcement.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class AnnouncementExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANNOUNCEMENT-404-01", "존재하지 않는 공지입니다."),

    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ASSIGNMENT-404-01", "존재하지 않는 과제입니다."),

    ANNOUNCEMENT_READ_NOT_FOUND(HttpStatus.NOT_FOUND, "ANNOUNCEMENT-READ-404-01", "존재하지 않는 공지/과제 읽기 이력입니다."),
    ANNOUNCEMENT_READ_MARKED_EXIST(HttpStatus.BAD_REQUEST, "ANNOUNCEMENT-READ-400-01", "이미 읽음 처리 된 공지/과제입니다."),
    TOKEN_INVALID(HttpStatus.BAD_REQUEST, "ANNOUNCEMENT-400-01", "유효하지 않은 토큰입니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
