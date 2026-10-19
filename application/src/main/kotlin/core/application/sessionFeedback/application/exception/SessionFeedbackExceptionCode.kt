package core.application.sessionFeedback.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class SessionFeedbackExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    FEEDBACK_START_AT_REQUIRED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-01", "피드백 시작 시간을 입력해주세요"),
    INVALID_FEEDBACK_START_AT(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-02", "피드백 시작 시간은 현재 이후여야 합니다"),
    FEEDBACK_DISABLED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-03", "피드백을 받지 않는 세션입니다"),
    FEEDBACK_NOT_STARTED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-04", "아직 피드백 응답 기간이 아닙니다"),
    FEEDBACK_CLOSED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-05", "피드백 응답 기간이 끝났습니다"),
    INVALID_ASPECT_COUNT(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-06", "항목은 1개 이상 2개 이하로 선택해주세요"),
    DUPLICATED_ASPECT(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-07", "같은 항목을 중복으로 선택할 수 없습니다"),
    EXCLUSIVE_ASPECT_COMBINED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-08", "'특별히 없음'은 다른 항목과 함께 선택할 수 없습니다"),
    ETC_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "SESSION_FEEDBACK-400-09", "기타 의견을 입력해주세요"),
    NOT_FEEDBACK_TARGET(HttpStatus.FORBIDDEN, "SESSION_FEEDBACK-403-01", "피드백 대상이 아닙니다"),
    ALREADY_SUBMITTED_FEEDBACK(HttpStatus.CONFLICT, "SESSION_FEEDBACK-409-01", "이미 피드백을 제출했습니다"),
    FEEDBACK_ALREADY_STARTED(HttpStatus.CONFLICT, "SESSION_FEEDBACK-409-02", "피드백 수집이 시작되어 설정을 변경할 수 없습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
