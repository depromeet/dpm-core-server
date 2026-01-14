package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class BillAccountExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SUCCESS(HttpStatus.OK, "BILL_ACCOUNT-000", "요청에 성공했습니다"),
    CREATE(HttpStatus.CREATED, "BILL_ACCOUNT-001", "리소스 생성에 성공했습니다"),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BILL-500", "예상치 못한 서버 에러가 발생했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "BILL_ACCOUNT-400", "올바른 입력 형식이 아닙니다."),

    BILL_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "BILL_ACCOUNT-404-01", "존재하지 않는 회식계좌입니다."),
    BILL_ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BILL_ACCOUNT-404-02", "BillAccount ID는 null일 수 없습니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
