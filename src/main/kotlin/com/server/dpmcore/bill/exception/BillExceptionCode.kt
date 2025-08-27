package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class BillExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SUCCESS(HttpStatus.OK, "BILL-200-01", "요청에 성공했습니다"),
    CREATE(HttpStatus.CREATED, "BILL-201-01", "리소스 생성에 성공했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "BILL-400-01", "올바른 입력 형식이 아닙니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BILL-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    BILL_NOT_FOUND(HttpStatus.BAD_REQUEST, "BILL-404-01", "존재하지 않는 정산입니다."),
    BILL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BILL-404-02", "Bill ID는 null일 수 없습니다."),
    BILL_CANNOT_CLOSE_PARTICIPATION(
        HttpStatus.BAD_REQUEST,
        "BILL-400-02",
        "현재 정산이 참여 가능한 상태(OPEN)가 아니기에 정산 참여를 마감할 수 없습니다.",
    ),
    BILL_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "BILL-400-03", "이미 완료된 정산입니다."),
    BILL_ALREADY_PARTICIPATION_CLOSED(HttpStatus.BAD_REQUEST, "BILL-400-04", "이미 참여 마감된 정산입니다."),
    BILL_ALREADY_PENDING(HttpStatus.BAD_REQUEST, "BILL-400-05", "이미 PENDING 상태입니다."),
    BILL_ALREADY_OPEN(HttpStatus.BAD_REQUEST, "BILL-400-06", "이미 OPEN 상태입니다."),
    BILL_ALREADY_IN_PROGRESS(HttpStatus.BAD_REQUEST, "BILL-400-07", "이미 IN_PROGRESS 상태입니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
