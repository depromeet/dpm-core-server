package core.application.attendance.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class AttendanceExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    INVALID_ATTENDANCE_ID(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-01", "유효하지 않은 출석 아이디입니다"),
    ABSENCE_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-02", "결석 사유를 입력해주세요"),
    ABSENCE_REASON_TOO_LONG(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-03", "결석 사유는 50자 이하로 입력해주세요"),
    INVALID_ABSENCE_REASON_IMAGE(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-04", "첨부할 수 없는 이미지입니다"),
    DUPLICATE_ABSENCE_REASON_IMAGE(HttpStatus.BAD_REQUEST, "ATTENDANCE-400-05", "같은 이미지를 중복해서 첨부할 수 없습니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ATTENDANCE-404-01", "출석을 찾을 수 없습니다"),
    ABSENCE_REASON_IMAGE_ALREADY_ATTACHED(HttpStatus.CONFLICT, "ATTENDANCE-409-02", "이미 다른 결석 사유서에 첨부된 이미지입니다"),
    ABSENCE_REASON_NOT_FOUND(HttpStatus.NOT_FOUND, "ATTENDANCE-404-02", "결석 사유서를 찾을 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}
