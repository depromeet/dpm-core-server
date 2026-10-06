package core.domain.sessionFeedback.enums

/**
 * 세션 피드백 선택형 문항 구분. 저장 시 어떤 문항의 선택인지 식별하는 데 사용된다.
 */
enum class SessionFeedbackQuestion {
    /** 좋았던 점. */
    LIKED,

    /** 개선이 필요한 점. */
    IMPROVEMENT,
    ;
}
