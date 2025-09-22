package core.application.gathering.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class GatheringExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GATHERING-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING-404-01", "존재하지 않는 회식입니다."),
    GATHERING_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING-400-02", "회식은 필수로 존재해야합니다."),
    GATHERING_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING-400-03", "회식 ID는 필수로 존재해야합니다."),
    GATHERING_NOT_INCLUDED_IN_BILL(HttpStatus.BAD_REQUEST, "GATHERING-400-04", "해당 정산에 포함되지 않은 회식입니다."),
    GATHERING_NOT_PARTICIPANT_MEMBER(HttpStatus.BAD_REQUEST, "GATHERING-400-05", "정산에 참여하지 않은 멤버입니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
