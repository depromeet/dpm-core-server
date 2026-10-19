package core.application.session.presentation.response

import core.domain.session.enums.NextSessionHomeStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 다음 세션 조회 응답")
data class NextSessionHomeResponse(
    @field:Schema(
        description = "홈 세션 카드 상태",
        example = "AVAILABLE",
        allowableValues = ["AVAILABLE", "NOT_REGISTERED", "COHORT_ENDED"],
    )
    val status: NextSessionHomeStatus,
    @field:Schema(description = "기수 value (문구용, 예: 19)", example = "19", nullable = true)
    val cohortValue: String?,
    @field:Schema(description = "다음 세션. AVAILABLE일 때만 존재", nullable = true)
    val session: NextSessionResponse?,
)
