package com.server.dpmcore.bill.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class BillExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SUCCESS(HttpStatus.OK, "BILL-000", "요청에 성공했습니다"),
    CREATE(HttpStatus.CREATED, "BILL-001", "리소스 생성에 성공했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "BILL-400", "올바른 입력 형식이 아닙니다."),
    BILL_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "BILL-401", "존재하지 않는 회식계좌입니다."),
    GATHERING_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING-402", "회식은 필수로 존재해야합니다."),
    BILL_ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BILL-403", "BillAccount ID는 null일 수 없습니다."),
    BILL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BILL-404", "Bill ID는 null일 수 없습니다."),
    GATHERING_MEMBERS_REQUIRED(HttpStatus.BAD_REQUEST, "BILL-405", "회식 참여 멤버는 필수로 존재해야합니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BILL-500", "예상치 못한 서버 에러가 발생했습니다"),
    BILL_NOT_FOUND(HttpStatus.BAD_REQUEST, "BILL-404-01", "존재하지 않는 정산입니다."),
    GATHERING_NOT_FOUND(HttpStatus.BAD_REQUEST, "GATHERING-404-01", "존재하지 않는 회식입니다."),
    BILL_CANNOT_CLOSE_PARTICIPATION(
        HttpStatus.BAD_REQUEST,
        "BILL-406",
        "현재 정산이 참여 가능한 상태(OPEN)가 아니기에 정산 참여를 마감할 수 없습니다.",
    ),
    GATHERING_RECEIPT_NOT_FOUND(HttpStatus.BAD_REQUEST, "GATHERING-404-02", "존재하지 않는 회식 영수증입니다."),
    GATHERING_RECEIPT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING-404-03", "회식 영수증 ID는 null일 수 없습니다."),
    BILL_ALREADY_PARTICIPATION_CLOSED(HttpStatus.BAD_REQUEST, "BILL-400-01", "이미 참여 마감된 정산입니다."),
    BILL_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "BILL-400-02", "이미 완료된 정산입니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
