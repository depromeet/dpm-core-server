package core.domain.sessionFeedback.enums

/**
 * 세션 피드백 선택 항목.
 *
 * `좋았던 점` / `개선이 필요한 점` 문항에서 선택 가능한 공통 후보. 문항별 노출 여부는
 * API 레이어에서 결정하며, 저장 시에는 어떤 문항인지 [SessionFeedbackQuestion] 과 함께 보관한다.
 */
enum class SessionFeedbackAspect(
    val requiresText: Boolean = false,
    val exclusive: Boolean = false,
) {
    SESSION_CONTENT,
    PROGRESS_AND_TIME,
    NETWORKING,
    GUIDANCE,
    PLACE_AND_ACCESS,
    ETC(requiresText = true),
    NOTHING(exclusive = true),
    ;
}
