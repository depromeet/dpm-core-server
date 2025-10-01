package core.application.gathering.application.exception.receipt

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class GatheringReceiptExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GATHERING_RECEIPT-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    GATHERING_RECEIPT_NOT_FOUND(HttpStatus.BAD_REQUEST, "GATHERING_RECEIPT-404-01", "존재하지 않는 회식 영수증입니다."),
    GATHERING_RECEIPT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING_RECEIPT-404-02", "회식 영수증 ID는 null일 수 없습니다."),

    MEMBER_COUNT_MUST_OVER_ONE(HttpStatus.BAD_REQUEST, "GATHERING_RECEIPT-400-02", "회식 참여 멤버 수는 1명 이상이어야 합니다."),
    RECEIPT_ALREADY_SPLIT(HttpStatus.BAD_REQUEST, "GATHERING_RECEIPT-400-03", "이미 분할 금액이 설정된 영수증입니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
