package core.application.gathering.application.exception.member

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class GatheringMemberExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GATHERING_MEMBER-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    GATHERING_MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "GATHERING_MEMBER-404-01", "존재하지 않는 회식 참여 멤버입니다."),
    GATHERING_MEMBER_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING_MEMBER-404-02", "회식 참여 멤버는 필수로 존재해야합니다."),
    ALREADY_CONFIRMED_MEMBER(HttpStatus.BAD_REQUEST, "GATHERING_MEMBER-400-02", "이미 정산 참여가 확정된 멤버는 참여 여부를 변경할 수 없습니다."),
    GATHERING_MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GATHERING_MEMBER-400-03", "회식 참여 멤버 ID는 필수로 존재해야합니다."),
    ALREADY_SUBMITTED_INVITATION(HttpStatus.BAD_REQUEST, "GATHERING_MEMBER-400-04", "이미 초대 답변이 제출된 회식 참여 멤버입니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
