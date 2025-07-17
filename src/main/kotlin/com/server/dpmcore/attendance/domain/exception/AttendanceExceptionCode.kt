package com.server.dpmcore.attendance.domain.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class AttendanceExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "A404", "출석을 찾을 수 없습니다"),
    ATTENDANCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A409", "이미 존재하는 출석입니다"),
    INVALID_ATTENDANCE_STATE(HttpStatus.BAD_REQUEST, "A400", "유효하지 않은 출석 상태입니다"),
    INVALID_ATTENDANCE_CODE(HttpStatus.BAD_REQUEST, "A400", "출석코드가 일치하지 않습니다"),
    TOO_EARLY_ATTENDANCE(HttpStatus.BAD_REQUEST, "A400", "출석하기에는 너무 이른 시간입니다"),
    ALREADY_CHECKED_ATTENDANCE(HttpStatus.BAD_REQUEST, "A400", "이미 출석을 체크했습니다"),
    ATTENDANCE_START_TIME_DATE_MISMATCH(HttpStatus.BAD_REQUEST, "A400", "출석 시작 시간의 날짜가 세션의 날짜와 일치하지 않습니다"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}
