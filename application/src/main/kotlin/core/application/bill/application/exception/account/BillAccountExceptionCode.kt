package core.application.bill.application.exception.account

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class BillAccountExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    BILL_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "BILL_ACCOUNT-404-01", "존재하지 않는 회식계좌입니다."),
    BILL_ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BILL_ACCOUNT-404-02", "BillAccount ID는 null일 수 없습니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
