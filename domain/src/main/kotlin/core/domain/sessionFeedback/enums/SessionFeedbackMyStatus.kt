package core.domain.sessionFeedback.enums

enum class SessionFeedbackMyStatus {
    AVAILABLE,
    BEFORE_START,
    SUBMITTED,
    EXPIRED,
    /** 출석 명단에 있으나 status=PENDING. FE: "세션 출석이 완료되지 않았어요." */
    ATTENDANCE_INCOMPLETE,
    NOT_TARGET,
}
