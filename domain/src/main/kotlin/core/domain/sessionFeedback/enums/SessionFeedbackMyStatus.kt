package core.domain.sessionFeedback.enums

/**
 * 로그인한 멤버 기준의 세션 피드백 응답 가능 상태.
 *
 * 판정 우선순위는 `DISABLED` → `NOT_TARGET` → `SUBMITTED` → `BEFORE_START` → `CLOSED` → `AVAILABLE`.
 */
enum class SessionFeedbackMyStatus {
    /** 설문 작성 가능. */
    AVAILABLE,

    /** 수집 시작 전. */
    BEFORE_START,

    /** 이미 제출함. */
    SUBMITTED,

    /** 수집 종료됨. */
    CLOSED,

    /** 출석/지각 대상이 아님. */
    NOT_TARGET,

    /** 세션의 피드백 받기 OFF. */
    DISABLED,
    ;
}
