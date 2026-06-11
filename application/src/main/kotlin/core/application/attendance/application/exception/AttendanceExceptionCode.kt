package core.application.attendance.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class AttendanceExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    INVALID_ATTENDANCE_ID(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-01", "유효하지 않은 출석 아이디입니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ATTENDANCE-404-01", "출석을 찾을 수 없습니다"),
    MEMBER_ATTENDANCE_AMBIGUOUS(HttpStatus.CONFLICT, "ATTENDANCE-409-01", "멤버 출석 정보를 특정할 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}
